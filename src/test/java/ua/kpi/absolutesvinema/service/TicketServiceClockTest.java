package ua.kpi.absolutesvinema.service;

import org.junit.jupiter.api.Test;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.repository.memory.InMemoryScreeningRepository;
import ua.kpi.absolutesvinema.repository.memory.InMemorySeatRepository;
import ua.kpi.absolutesvinema.repository.memory.InMemoryTicketRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TicketServiceClockTest {
    @Test
    void bookingUsesInjectedClock() {
        var clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.ofHours(2));
        var screenings = new InMemoryScreeningRepository();
        var seats = new InMemorySeatRepository();
        var tickets = new InMemoryTicketRepository();
        var screening = screenings.save(new Screening(null, "Film", "Description", 100,
                LocalDateTime.now(clock).plusDays(1), "Hall", BigDecimal.TEN));
        var seat = seats.save(new Seat(null, screening.id(), 1, 1));
        var service = new TicketService(tickets, seats, screenings, clock);

        var ticket = service.book(seat.id(), "Anna");

        assertThat(ticket.bookedAt()).isEqualTo(LocalDateTime.of(2026, 11, 15, 14, 0));
        assertThat(service.findById(ticket.id())).isEqualTo(ticket);
    }
}
