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
import ua.kpi.absolutesvinema.service.ScreeningService;
import ua.kpi.absolutesvinema.service.TicketService;
import ua.kpi.absolutesvinema.service.UserService;

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
class UserTicketBookingTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedAccessToMyTicketsRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/tickets/my"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "user@svinema.ua", roles = {"USER"})
    void authenticatedUserCanBookAndManageTickets() throws Exception {
        var user = userService.findByEmail("user@svinema.ua").orElseThrow();

        Screening screening = screeningService.create(
                "Dune: Part Two",
                "Paul Atreides unites with the Fremen people.",
                166,
                LocalDateTime.now().plusDays(4),
                "IMAX",
                new BigDecimal("250.00")
        );

        Seat seat = screeningService.findSeats(screening.id()).getFirst();

        // Book seat as logged in user
        mockMvc.perform(post("/screenings/" + screening.id() + "/book").with(csrf())
                        .param("seatId", seat.id().toString())
                        .param("customerName", "Danylo Marchenko"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/screenings/" + screening.id()));

        // Check ticket is linked to user
        var userTickets = ticketService.findUserTicketsWithDetails(user.id());
        assertThat(userTickets).hasSizeGreaterThanOrEqualTo(1);
        var bookedTicket = userTickets.stream()
                .filter(t -> t.ticket().seatId().equals(seat.id()))
                .findFirst()
                .orElseThrow();
        assertThat(bookedTicket.ticket().userId()).isEqualTo(user.id());

        // Check /tickets/my displays the booked ticket
        mockMvc.perform(get("/tickets/my"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dune: Part Two")))
                .andExpect(content().string(containsString("Row <span>" + seat.rowNumber() + "</span>")))
                .andExpect(content().string(containsString("250.00")));

        // Cancel the ticket
        mockMvc.perform(post("/tickets/" + bookedTicket.ticket().id() + "/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets/my"));

        // Verify seat is available again
        assertThat(ticketService.isSeatBooked(seat.id())).isFalse();
    }
    @Test
    void anonymousCancellationRequiresLoginEvenWithCsrf() throws Exception {
        mockMvc.perform(post("/tickets/1/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "user@svinema.ua")
    void otherUsersAndGuestTicketsCannotBeCancelled() throws Exception {
        var screening = screeningService.create("Ownership", "Test", 90,
                LocalDateTime.now().plusDays(1), "Hall", BigDecimal.TEN);
        var seats = screeningService.findSeats(screening.id());
        var other = userService.findByEmail("admin@svinema.ua").orElseThrow();
        var otherTicket = ticketService.book(seats.get(0).id(), other.id(), "Other owner");
        var guestTicket = ticketService.book(seats.get(1).id(), "Guest owner");
        for (var ticket : java.util.List.of(otherTicket, guestTicket)) {
            mockMvc.perform(post("/tickets/" + ticket.id() + "/cancel").with(csrf()))
                    .andExpect(redirectedUrl("/tickets/my"))
                    .andExpect(flash().attribute("error", "You cannot cancel another user's ticket."));
            assertThat(ticketService.findById(ticket.id())).isEqualTo(ticket);
        }
        mockMvc.perform(get("/tickets/my"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Other owner"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Guest owner"))));
    }

    @Test
    @WithMockUser(username = "missing@example.com")
    void unknownAuthenticatedUserIsDenied() throws Exception {
        mockMvc.perform(get("/tickets/my")).andExpect(status().isForbidden());
        mockMvc.perform(post("/tickets/1/cancel").with(csrf())).andExpect(status().isForbidden());
    }

}
