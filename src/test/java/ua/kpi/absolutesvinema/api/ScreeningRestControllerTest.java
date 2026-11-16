package ua.kpi.absolutesvinema.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.service.ScreeningService;
import ua.kpi.absolutesvinema.service.TicketService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ScreeningRestControllerTest {

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
    void listScreeningsIsPublicAndReturnsJsonArray() throws Exception {
        Screening sample = screeningService.create(
                "Public Film " + System.currentTimeMillis(),
                "Description",
                120,
                LocalDateTime.of(2027, 6, 1, 15, 0),
                "Hall A",
                new BigDecimal("180.00")
        );

        mockMvc.perform(get("/api/screenings"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", isA(List.class)))
                .andExpect(jsonPath("$[?(@.id == " + sample.id() + ")].movieTitle").value(sample.movieTitle()));
    }

    @Test
    void getScreeningByIdReturnsResponseOr404() throws Exception {
        Screening sample = screeningService.create(
                "Details Film " + System.currentTimeMillis(),
                "Detailed Description",
                95,
                LocalDateTime.of(2027, 7, 10, 21, 0),
                "Hall B",
                new BigDecimal("210.00")
        );

        mockMvc.perform(get("/api/screenings/" + sample.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(sample.id()))
                .andExpect(jsonPath("$.movieTitle").value(sample.movieTitle()))
                .andExpect(jsonPath("$.movieDurationMinutes").value(95))
                .andExpect(jsonPath("$.hall").value("Hall B"));

        mockMvc.perform(get("/api/screenings/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void createScreeningSucceedsForAdminWithLocationAndCreates15Seats() throws Exception {
        String payload = """
                {
                  "movieTitle": "Dune: Part Two",
                  "movieDescription": "Paul Atreides unites with Chani and the Fremen",
                  "movieDurationMinutes": 166,
                  "startsAt": "2027-11-20T19:30:00",
                  "hall": "IMAX Laser",
                  "ticketPrice": 250.00
                }
                """;

        var result = mockMvc.perform(post("/api/screenings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/screenings/\\d+")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.movieTitle").value("Dune: Part Two"))
                .andExpect(jsonPath("$.hall").value("IMAX Laser"))
                .andExpect(jsonPath("$.movieDurationMinutes").value(166))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        Long createdId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        // Verify that ScreeningService automatically created 15 seats
        List<Seat> seats = screeningService.findSeats(createdId);
        assertThat(seats).hasSize(15);
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void createScreeningFailsOnInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                  "movieTitle": "",
                  "movieDescription": "Desc",
                  "movieDurationMinutes": -5,
                  "startsAt": null,
                  "hall": "   ",
                  "ticketPrice": -50.00
                }
                """;

        mockMvc.perform(post("/api/screenings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid Request Content"))
                .andExpect(jsonPath("$.errors.movieTitle", notNullValue()))
                .andExpect(jsonPath("$.errors.movieDurationMinutes", notNullValue()))
                .andExpect(jsonPath("$.errors.startsAt", notNullValue()))
                .andExpect(jsonPath("$.errors.hall", notNullValue()))
                .andExpect(jsonPath("$.errors.ticketPrice", notNullValue()));
    }

    @Test
    void createScreeningRequiresAuthenticationAndAdminRole() throws Exception {
        String payload = """
                {
                  "movieTitle": "Restricted Film",
                  "movieDescription": "Desc",
                  "movieDurationMinutes": 100,
                  "startsAt": "2027-12-01T20:00:00",
                  "hall": "Hall 1",
                  "ticketPrice": 100.00
                }
                """;

        // Anonymous without CSRF -> 403 (CSRF blocked)
        mockMvc.perform(post("/api/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        // Anonymous with CSRF -> 401 Unauthorized
        mockMvc.perform(post("/api/screenings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    @WithMockUser(username = "user@svinema.ua", roles = "USER")
    void createScreeningForbiddenForRegularUser() throws Exception {
        String payload = """
                {
                  "movieTitle": "Restricted Film",
                  "movieDescription": "Desc",
                  "movieDurationMinutes": 100,
                  "startsAt": "2027-12-01T20:00:00",
                  "hall": "Hall 1",
                  "ticketPrice": 100.00
                }
                """;

        mockMvc.perform(post("/api/screenings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void updateScreeningFullyUpdatesExistingScreening() throws Exception {
        Screening original = screeningService.create(
                "Original Title",
                "Original Description",
                100,
                LocalDateTime.of(2027, 4, 1, 12, 0),
                "Old Hall",
                new BigDecimal("120.00")
        );

        String updatePayload = """
                {
                  "movieTitle": "Updated Title",
                  "movieDescription": "Updated Description",
                  "movieDurationMinutes": 135,
                  "startsAt": "2027-04-01T15:00:00",
                  "hall": "New Hall",
                  "ticketPrice": 160.00
                }
                """;

        mockMvc.perform(put("/api/screenings/" + original.id())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id()))
                .andExpect(jsonPath("$.movieTitle").value("Updated Title"))
                .andExpect(jsonPath("$.movieDescription").value("Updated Description"))
                .andExpect(jsonPath("$.movieDurationMinutes").value(135))
                .andExpect(jsonPath("$.hall").value("New Hall"))
                .andExpect(jsonPath("$.ticketPrice").value(160.00));

        Screening updated = screeningService.findById(original.id());
        assertThat(updated.movieTitle()).isEqualTo("Updated Title");
        assertThat(updated.hall()).isEqualTo("New Hall");
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void updateScreeningReturns404ForAbsentScreening() throws Exception {
        String updatePayload = """
                {
                  "movieTitle": "Ghost Film",
                  "movieDescription": "Does not exist",
                  "movieDurationMinutes": 90,
                  "startsAt": "2027-04-01T15:00:00",
                  "hall": "Hall",
                  "ticketPrice": 100.00
                }
                """;

        mockMvc.perform(put("/api/screenings/999999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void deleteScreeningReturns204AndCascadesDeletionOfSeatsAndTickets() throws Exception {
        Screening screening = screeningService.create(
                "To Delete",
                "Description",
                110,
                LocalDateTime.of(2027, 8, 1, 17, 0),
                "Hall D",
                new BigDecimal("150.00")
        );
        Long screeningId = screening.id();
        Seat firstSeat = screeningService.findSeats(screeningId).getFirst();

        // Book a ticket to verify cascade deletion
        ticketService.book(firstSeat.id(), null, "Test Customer");
        assertThat(ticketService.isSeatBooked(firstSeat.id())).isTrue();

        mockMvc.perform(delete("/api/screenings/" + screeningId)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Subsequent GET must return 404
        mockMvc.perform(get("/api/screenings/" + screeningId))
                .andExpect(status().isNotFound());

        // Verify seats and tickets were cascaded: checking the deleted seat throws ResourceNotFoundException
        org.junit.jupiter.api.Assertions.assertThrows(
                ua.kpi.absolutesvinema.exception.ResourceNotFoundException.class,
                () -> ticketService.isSeatBooked(firstSeat.id())
        );
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void deleteAbsentScreeningReturns404() throws Exception {
        mockMvc.perform(delete("/api/screenings/999999")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
