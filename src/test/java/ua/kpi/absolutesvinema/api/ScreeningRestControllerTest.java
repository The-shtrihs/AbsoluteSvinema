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
    void listScreeningsIsPublicAndReturnsPagedResponse() throws Exception {
        Screening sample = screeningService.create(
                "Public Film " + System.currentTimeMillis(),
                "Description",
                120,
                LocalDateTime.of(2027, 6, 1, 15, 0),
                "Hall A",
                new BigDecimal("180.00")
        );

        mockMvc.perform(get("/api/screenings")
                        .param("title", sample.movieTitle()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", isA(List.class)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].movieTitle").value(sample.movieTitle()));
    }

    @Test
    void listScreeningsFilteringByTitleAndHall() throws Exception {
        String uniqueSuffix = "_" + System.currentTimeMillis();
        Screening s1 = screeningService.create("Interstellar" + uniqueSuffix, "Sci-Fi", 169,
                LocalDateTime.now().plusDays(2), "Hall 1", new BigDecimal("200.00"));
        Screening s2 = screeningService.create("Intermission" + uniqueSuffix, "Drama", 100,
                LocalDateTime.now().plusDays(3), "Hall 2", new BigDecimal("150.00"));
        Screening s3 = screeningService.create("Other" + uniqueSuffix, "Action", 90,
                LocalDateTime.now().plusDays(4), "Hall 1", new BigDecimal("120.00"));

        // Title substring case-insensitive with trim
        mockMvc.perform(get("/api/screenings")
                        .param("title", "  iNtEr  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + s1.id() + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + s2.id() + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + s3.id() + ")]").doesNotExist());

        // Hall exact match case-insensitive with trim
        mockMvc.perform(get("/api/screenings")
                        .param("hall", "  hAlL 1  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + s1.id() + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + s2.id() + ")]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id == " + s3.id() + ")]").exists());

        // Combined title and hall filter
        mockMvc.perform(get("/api/screenings")
                        .param("title", "inter")
                        .param("hall", "hall 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + s1.id() + ")]").exists())
                .andExpect(jsonPath("$.content[?(@.id == " + s2.id() + ")]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id == " + s3.id() + ")]").doesNotExist());
    }

    @Test
    void listScreeningsStableOrderingByIdAndPaginationSlices() throws Exception {
        String unique = "Slice_" + System.currentTimeMillis();
        Screening s1 = screeningService.create(unique + " A", "D1", 100, LocalDateTime.now().plusDays(1), "H1", BigDecimal.TEN);
        Screening s2 = screeningService.create(unique + " B", "D2", 100, LocalDateTime.now().plusDays(1), "H1", BigDecimal.TEN);
        Screening s3 = screeningService.create(unique + " C", "D3", 100, LocalDateTime.now().plusDays(1), "H1", BigDecimal.TEN);

        // Page 0 size 2 -> must contain s1 and s2 in ascending id order
        mockMvc.perform(get("/api/screenings")
                        .param("title", unique)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(s1.id()))
                .andExpect(jsonPath("$.content[1].id").value(s2.id()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Page 1 size 2 -> must contain s3
        mockMvc.perform(get("/api/screenings")
                        .param("title", unique)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(s3.id()))
                .andExpect(jsonPath("$.page").value(1));

        // Out of range page -> returns 200 with empty content and correct totals
        mockMvc.perform(get("/api/screenings")
                        .param("title", unique)
                        .param("page", "5")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(5))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listScreeningsZeroResultsReturns200WithEmptyContent() throws Exception {
        mockMvc.perform(get("/api/screenings")
                        .param("title", "NonexistentTitleXYZ123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void listScreeningsRejectsInvalidPageAndSize() throws Exception {
        // Negative page -> 400
        mockMvc.perform(get("/api/screenings").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));

        // Size 0 -> 400
        mockMvc.perform(get("/api/screenings").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));

        // Size 101 -> 400
        mockMvc.perform(get("/api/screenings").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));

        // Type mismatch for page -> 400
        mockMvc.perform(get("/api/screenings").param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
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
