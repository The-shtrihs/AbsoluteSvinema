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
        mockMvc.perform(post("/screenings/" + screening.id() + "/book")
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
        mockMvc.perform(post("/tickets/" + bookedTicket.ticket().id() + "/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets/my"));

        // Verify seat is available again
        assertThat(ticketService.isSeatBooked(seat.id())).isFalse();
    }
}
