package ua.kpi.absolutesvinema.repository.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.model.User;
import ua.kpi.absolutesvinema.repository.TicketRepository;
import ua.kpi.absolutesvinema.repository.UserRepository;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class JdbcTicketUserRepositoryTest {

    private static final LocalDateTime TIME = LocalDateTime.of(2027, 1, 10, 12, 0);

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private UserRepository users;

    @Autowired
    private ScreeningService screeningService;

    private List<Long> newSeatIds() {
        var screening = screeningService.create("Ticket Film " + System.nanoTime(), "Description", 90,
                TIME.plusDays(1), "Hall 1", BigDecimal.TEN);
        return screeningService.findSeats(screening.id()).stream().map(Seat::id).toList();
    }

    @Test
    void jdbcRepositoriesAreActive() {
        assertThat(tickets).isInstanceOf(JdbcTicketRepository.class);
        assertThat(users).isInstanceOf(JdbcUserRepository.class);
    }

    @Test
    void ticketCrudAndSearchesWithGuestAndUser() {
        var seatIds = newSeatIds();
        User owner = users.save(new User(null, "owner" + System.nanoTime() + "@test.ua", "hash", "Owner", "ROLE_USER"));

        Ticket guest = tickets.save(new Ticket(null, seatIds.get(0), null, "Guest", TIME));
        Ticket owned = tickets.save(new Ticket(null, seatIds.get(1), owner.id(), "Owner", TIME));

        assertThat(guest.id()).isPositive();
        assertThat(guest.userId()).isNull();
        assertThat(tickets.findById(guest.id())).contains(guest);
        assertThat(tickets.findBySeatId(seatIds.get(1))).contains(owned);
        assertThat(tickets.findByUserId(owner.id())).containsExactly(owned);
        assertThat(tickets.findBySeatIdIn(seatIds)).containsExactly(guest, owned);
        assertThat(tickets.findBySeatIdIn(List.of())).isEmpty();

        Ticket renamed = tickets.save(new Ticket(guest.id(), guest.seatId(), null, "Renamed", TIME));
        assertThat(tickets.findById(guest.id())).contains(renamed);

        assertThatThrownBy(() -> tickets.save(new Ticket(guest.id(), seatIds.get(2), null, "Moved", TIME)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tickets.save(new Ticket(999_999L, seatIds.get(2), null, "Ghost", TIME)))
                .isInstanceOf(IllegalArgumentException.class);

        tickets.deleteById(guest.id());
        assertThat(tickets.findById(guest.id())).isEmpty();
    }

    @Test
    void uniqueSeatConstraintBecomesSeatConflict() {
        Long seatId = newSeatIds().getFirst();
        tickets.save(new Ticket(null, seatId, null, "First", TIME));

        assertThatThrownBy(() -> tickets.save(new Ticket(null, seatId, null, "Second", TIME)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat already has a ticket");
        assertThat(tickets.findBySeatIdIn(List.of(seatId))).hasSize(1);
    }

    @Test
    void userCrudFindByNormalizedEmailAndUniqueEmail() {
        String email = "crud" + System.nanoTime() + "@test.ua";
        User saved = users.save(new User(null, email, "hash", "Crud User", "ROLE_USER"));

        assertThat(saved.id()).isPositive();
        assertThat(users.findByEmail("  " + email.toUpperCase() + " ")).contains(saved);

        User updated = users.save(new User(saved.id(), email, "hash2", "Crud User 2", "ROLE_ADMIN"));
        assertThat(users.findById(saved.id())).contains(updated);

        assertThatThrownBy(() -> users.save(new User(null, email.toUpperCase(), "hash", "Dup", "ROLE_USER")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> users.save(new User(999_999L, "ghost@test.ua", "hash", "Ghost", "ROLE_USER")))
                .isInstanceOf(IllegalArgumentException.class);

        users.deleteById(saved.id());
        assertThat(users.findById(saved.id())).isEmpty();
    }
}
