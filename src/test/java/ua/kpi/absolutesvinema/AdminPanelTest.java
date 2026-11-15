package ua.kpi.absolutesvinema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.service.ScreeningService;
import ua.kpi.absolutesvinema.service.TicketService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AdminPanelTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private TicketService ticketService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedAccessToAdminIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/screenings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/admin/tickets"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "user@svinema.ua", roles = {"USER"})
    void regularUserCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/admin/screenings"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/tickets"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/screenings/1/delete").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = {"ADMIN"})
    void adminCanManageScreenings() throws Exception {
        // List screenings
        mockMvc.perform(get("/admin/screenings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Screenings Management")));

        // New screening form
        mockMvc.perform(get("/admin/screenings/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create New Screening")));

        // Validation error on blank title
        mockMvc.perform(post("/admin/screenings").with(csrf())
                        .param("movieTitle", "  ")
                        .param("movieDescription", "Some desc")
                        .param("movieDurationMinutes", "100")
                        .param("startsAt", "2027-05-20T19:00")
                        .param("hall", "Hall A")
                        .param("ticketPrice", "150.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/screenings/form"))
                .andExpect(content().string(containsString("Movie title must not be blank")));

        // Create valid screening
        String uniqueTitle = "Admin Created Movie " + System.currentTimeMillis();
        mockMvc.perform(post("/admin/screenings").with(csrf())
                        .param("movieTitle", uniqueTitle)
                        .param("movieDescription", "Created by admin")
                        .param("movieDurationMinutes", "145")
                        .param("startsAt", "2027-05-20T19:00")
                        .param("hall", "IMAX")
                        .param("ticketPrice", "210.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/screenings"));

        Screening created = screeningService.findAll().stream()
                .filter(s -> s.movieTitle().equals(uniqueTitle))
                .findFirst()
                .orElseThrow();

        assertThat(screeningService.findSeats(created.id())).hasSize(15);

        // Edit form
        mockMvc.perform(get("/admin/screenings/" + created.id() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit Screening #" + created.id())));

        // Update screening
        mockMvc.perform(post("/admin/screenings/" + created.id() + "/edit").with(csrf())
                        .param("movieTitle", uniqueTitle + " (Updated)")
                        .param("movieDescription", "Updated description")
                        .param("movieDurationMinutes", "150")
                        .param("startsAt", "2027-05-20T20:00")
                        .param("hall", "VIP")
                        .param("ticketPrice", "230.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/screenings"));

        assertThat(screeningService.findById(created.id()).movieTitle()).contains("(Updated)");

        // Delete screening
        mockMvc.perform(post("/admin/screenings/" + created.id() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/screenings"));

        assertThat(screeningService.findAll().stream().noneMatch(s -> s.id().equals(created.id()))).isTrue();
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = {"ADMIN"})
    void adminCanManageTickets() throws Exception {
        Screening screening = screeningService.create(
                "Ticket Admin Test Film",
                "Testing admin ticket features",
                95,
                LocalDateTime.now().plusDays(2),
                "Hall 1",
                new BigDecimal("120.00")
        );
        Seat seat = screeningService.findSeats(screening.id()).getFirst();
        Ticket ticket = ticketService.book(seat.id(), "Original Customer");

        // List tickets
        mockMvc.perform(get("/admin/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Original Customer")))
                .andExpect(content().string(containsString("Ticket Admin Test Film")));

        // Edit form
        mockMvc.perform(get("/admin/tickets/" + ticket.id() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit Ticket #" + ticket.id())));

        // Update customer name
        mockMvc.perform(post("/admin/tickets/" + ticket.id() + "/edit").with(csrf())
                        .param("customerName", "Renamed Customer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"));

        assertThat(ticketService.findById(ticket.id()).customerName()).isEqualTo("Renamed Customer");

        // Cancel ticket
        mockMvc.perform(post("/admin/tickets/" + ticket.id() + "/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"));

        assertThat(ticketService.isSeatBooked(seat.id())).isFalse();
    }
    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void allRenderedPostFormsContainExactlyOneCsrfToken() throws Exception {
        var screening = screeningService.create("CSRF forms", "Test", 90,
                LocalDateTime.now().plusDays(1), "Hall", BigDecimal.TEN);
        var admin = context.getBean(ua.kpi.absolutesvinema.service.UserService.class)
                .findByEmail("admin@svinema.ua").orElseThrow();
        var ticket = ticketService.book(screeningService.findSeats(screening.id()).getFirst().id(),
                admin.id(), "CSRF owner");
        for (String path : java.util.List.of("/login", "/register", "/", "/screenings/" + screening.id(),
                "/tickets/my", "/admin/screenings", "/admin/screenings/new",
                "/admin/screenings/" + screening.id() + "/edit", "/admin/tickets",
                "/admin/tickets/" + ticket.id() + "/edit")) {
            var result = mockMvc.perform(get(path)).andExpect(status().isOk()).andReturn();
            var forms = java.util.regex.Pattern.compile("<form\\b[^>]*>.*?</form>",
                    java.util.regex.Pattern.DOTALL).matcher(result.getResponse().getContentAsString());
            int count = 0;
            while (forms.find()) {
                String form = forms.group();
                if (!form.contains("method=\"post\"")) continue;
                count++;
                var tokens = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                        .matcher(form);
                assertThat(tokens.find()).as("CSRF token in %s", path).isTrue();
                assertThat(tokens.group(1)).isEqualTo(((org.springframework.security.web.csrf.CsrfToken)
                        result.getRequest().getAttribute("_csrf")).getToken());
                assertThat(tokens.find()).as("No duplicate token in %s", path).isFalse();
            }
            assertThat(count).as("POST forms on %s", path).isPositive();
        }
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void postWithoutCsrfIsRejectedAcrossAllFormRoutes() throws Exception {
        for (String path : java.util.List.of("/login", "/register", "/logout", "/screenings/1/book",
                "/tickets/1/cancel", "/admin/screenings", "/admin/screenings/1/edit",
                "/admin/screenings/1/delete", "/admin/tickets/1/edit", "/admin/tickets/1/cancel")) {
            mockMvc.perform(post(path)).andExpect(status().isForbidden());
        }
    }

}
