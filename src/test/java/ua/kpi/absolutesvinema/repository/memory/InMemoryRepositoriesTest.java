package ua.kpi.absolutesvinema.repository.memory;

import org.junit.jupiter.api.Test;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.model.Ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class InMemoryRepositoriesTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 12, 1, 18, 0);

    @Test
    void screeningCrudAndGeneratedIds() {
        var repository = new InMemoryScreeningRepository();
        var draft = new Screening(null, "Film", "A short film description", 120, TIME, "Hall 1", BigDecimal.TEN);
        var first = repository.save(draft);
        var second = repository.save(draft);
        assertThat(draft.id()).isNull();
        assertThat(first.id()).isPositive().isNotEqualTo(second.id());
        assertThat(repository.findById(first.id())).contains(first);
        assertThat(first.movieDescription()).isEqualTo("A short film description");
        assertThat(first.movieDurationMinutes()).isEqualTo(120);
        var updated = new Screening(first.id(), "Updated", "An updated description", 95, TIME, "Hall 2", BigDecimal.ONE);
        assertThat(repository.save(updated)).isEqualTo(updated);
        assertThat(repository.findById(first.id())).contains(updated);
        assertThat(repository.findById(first.id()).orElseThrow().movieDescription())
                .isEqualTo("An updated description");
        assertThat(repository.findById(first.id()).orElseThrow().movieDurationMinutes()).isEqualTo(95);
        assertThat(repository.findAll()).containsExactly(updated, second);
        repository.deleteById(first.id());
        repository.deleteById(first.id());
        assertThat(repository.findById(first.id())).isEmpty();
        assertThat(repository.findAll()).containsExactly(second);
        assertThatThrownBy(() -> repository.save(first)).isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.save(draft).id()).isGreaterThan(second.id());
    }

    @Test
    void seatCrudAndSearchByScreening() {
        var repository = new InMemorySeatRepository();
        var first = repository.save(new Seat(null, 1L, 1, 1));
        var second = repository.save(new Seat(null, 2L, 1, 1));
        assertThat(first.id()).isPositive().isNotEqualTo(second.id());
        assertThat(repository.findById(first.id())).contains(first);
        var updated = new Seat(first.id(), 1L, 2, 3);
        assertThat(repository.save(updated)).isEqualTo(updated);
        assertThat(repository.findByScreeningId(1L)).containsExactly(updated);
        assertThat(repository.findByScreeningId(99L)).isEmpty();
        assertThat(repository.findAll()).containsExactly(updated, second);
        repository.deleteById(first.id());
        repository.deleteById(first.id());
        assertThat(repository.findById(first.id())).isEmpty();
        assertThat(repository.findAll()).containsExactly(second);
        assertThatThrownBy(() -> repository.save(first)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ticketCrudAndSearchBySeats() {
        var repository = new InMemoryTicketRepository();
        var first = repository.save(new Ticket(null, 1L, "Anna", TIME));
        var second = repository.save(new Ticket(null, 2L, "Ivan", TIME));
        assertThat(first.id()).isPositive().isNotEqualTo(second.id());
        assertThat(repository.findById(first.id())).contains(first);
        var updated = new Ticket(first.id(), first.seatId(), "Olena", TIME);
        assertThat(repository.save(updated)).isEqualTo(updated);
        assertThat(repository.save(updated)).isEqualTo(updated);
        assertThat(repository.findAll()).containsExactly(updated, second);
        assertThat(repository.findBySeatId(1L)).contains(updated);
        assertThat(repository.findBySeatId(99L)).isEmpty();
        assertThat(repository.findBySeatIdIn(List.of(1L, 2L, 99L))).containsExactly(updated, second);
        assertThat(repository.findBySeatIdIn(List.of(2L))).containsExactly(second);
        assertThat(repository.findBySeatIdIn(List.of())).isEmpty();
        repository.deleteById(first.id());
        repository.deleteById(first.id());
        assertThat(repository.findById(first.id())).isEmpty();
        assertThat(repository.findBySeatId(1L)).isEmpty();
        assertThatThrownBy(() -> repository.save(first)).isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.save(new Ticket(null, 1L, "New guest", TIME)).id())
                .isGreaterThan(second.id());
    }

    @Test
    void rejectsRepeatedBookingAndMovingExistingTicket() {
        var repository = new InMemoryTicketRepository();
        var draft = new Ticket(null, 1L, "Anna", TIME);
        var saved = repository.save(draft);
        assertThatThrownBy(() -> repository.save(draft)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> repository.save(new Ticket(null, 1L, "Ivan", TIME)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> repository.save(new Ticket(saved.id(), 2L, "Anna", TIME)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.findAll()).containsExactly(saved);
    }

    @Test
    void onlyOneConcurrentBookingSucceeds() throws Exception {
        var repository = new InMemoryTicketRepository();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> booking = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for start");
                }
                try {
                    repository.save(new Ticket(null, 1L, "Guest", TIME));
                    return true;
                } catch (IllegalStateException occupied) {
                    return false;
                }
            };
            var first = executor.submit(booking);
            var second = executor.submit(booking);
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findBySeatId(1L)).isPresent();
    }
}
