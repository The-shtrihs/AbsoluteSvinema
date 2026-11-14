package ua.kpi.absolutesvinema.config;

import org.junit.jupiter.api.Test;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.repository.memory.InMemoryScreeningRepository;
import ua.kpi.absolutesvinema.repository.memory.InMemorySeatRepository;
import ua.kpi.absolutesvinema.repository.memory.InMemoryTicketRepository;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataTest {
    @Test
    void initializesThreeScreeningsAndFortyFiveSeatsOnlyOnce() {
        var clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);
        var screenings = new InMemoryScreeningRepository();
        var seats = new InMemorySeatRepository();
        var tickets = new InMemoryTicketRepository();
        var service = new ScreeningService(screenings, seats, tickets);
        var demo = new DemoData(service, clock);

        demo.run();

        var initialScreenings = service.findAll();
        var initialSeats = seats.findAll();
        assertThat(initialScreenings).extracting(Screening::movieTitle).containsExactly(
                "Interstellar", "Pulp Fiction", "The Lord of the Rings: The Return of the King");
        assertThat(initialScreenings).extracting(Screening::movieDurationMinutes).containsExactly(169, 154, 201);
        assertThat(initialScreenings).extracting(Screening::hall).doesNotHaveDuplicates();
        assertThat(initialScreenings).allSatisfy(screening -> {
            assertThat(screening.movieDescription()).isNotBlank();
            assertThat(screening.ticketPrice()).isPositive();
            assertThat(screening.startsAt().toLocalDate()).isEqualTo(LocalDate.now(clock).plusDays(1));
            assertThat(service.findSeats(screening.id())).hasSize(15);
        });
        assertThat(initialSeats).hasSize(45);

        demo.run();

        assertThat(service.findAll()).containsExactlyElementsOf(initialScreenings);
        assertThat(seats.findAll()).containsExactlyElementsOf(initialSeats);
    }
}
