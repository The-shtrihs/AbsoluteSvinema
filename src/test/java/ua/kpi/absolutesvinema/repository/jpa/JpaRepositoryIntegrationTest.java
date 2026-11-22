package ua.kpi.absolutesvinema.repository.jpa;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.model.User;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.repository.TicketRepository;
import ua.kpi.absolutesvinema.repository.UserRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.ScreeningJpaDataRepository;
import ua.kpi.absolutesvinema.repository.jpa.store.SeatJpaDataRepository;
import ua.kpi.absolutesvinema.repository.jpa.store.TicketJpaDataRepository;
import ua.kpi.absolutesvinema.repository.jpa.store.UserJpaDataRepository;
import ua.kpi.absolutesvinema.repository.jdbc.JdbcScreeningRepository;
import ua.kpi.absolutesvinema.repository.memory.InMemoryScreeningRepository;
import ua.kpi.absolutesvinema.service.ScreeningService;
import ua.kpi.absolutesvinema.service.TicketService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.sql.init.mode=always")
@ActiveProfiles("jpa")
class JpaRepositoryIntegrationTest {
    @Autowired private ApplicationContext context;
    @Autowired private ScreeningRepository screenings;
    @Autowired private SeatRepository seats;
    @Autowired private TicketRepository tickets;
    @Autowired private UserRepository users;
    @Autowired private ScreeningJpaDataRepository screeningData;
    @Autowired private TicketJpaDataRepository ticketData;
    @Autowired private UserJpaDataRepository userData;
    @Autowired private ScreeningService screeningService;
    @Autowired private TicketService ticketService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void jpaProfileRegistersOnlyJpaRepositoryAdapters() {
        assertThat(screenings).isInstanceOf(ScreeningJpaAdapter.class);
        assertThat(seats).isInstanceOf(SeatJpaAdapter.class);
        assertThat(tickets).isInstanceOf(TicketJpaAdapter.class);
        assertThat(users).isInstanceOf(UserJpaAdapter.class);
        assertThat(context.getBeansOfType(JdbcScreeningRepository.class)).isEmpty();
        assertThat(context.getBeansOfType(InMemoryScreeningRepository.class)).isEmpty();
    }

    @Test
    void repositoryCrudGeneratesIdsAndMapsScreeningSeatsAndQueries() {
        String token = "JpaQuery" + System.nanoTime();
        Screening screening = screenings.save(new Screening(null, token + " 100%_", "Description", 100,
                LocalDateTime.of(2027, 1, 10, 18, 30), " Hall 8 ", new BigDecimal("125.50")));
        assertThat(screening.id()).isPositive();

        Screening updated = screenings.save(new Screening(screening.id(), token + " Updated 100%_", "Changed", 105,
                screening.startsAt().plusHours(1), " Hall 8 ", new BigDecimal("130.00")));
        assertThat(screenings.findById(screening.id())).contains(updated);

        var seatsForScreening = screeningService.create(token + " Relation", "Description", 90,
                screening.startsAt(), "Hall 9", BigDecimal.TEN);
        var orderedSeats = seats.findByScreeningId(seatsForScreening.id());
        assertThat(orderedSeats).hasSize(15);
        assertThat(orderedSeats).extracting("rowNumber").containsSubsequence(1, 1, 1, 1, 1, 2);

        Integer relatedSeatCount = new TransactionTemplate(transactionManager).execute(status -> {
            entityManager.clear();
            return entityManager.find(ScreeningEntity.class, seatsForScreening.id()).getSeats().size();
        });
        assertThat(relatedSeatCount).isEqualTo(15);

        assertThat(screeningData.findByTitleAndHall("100%_", "hall 8"))
                .extracting(ScreeningEntity::getId).containsExactly(screening.id());
        assertThat(screeningData.findByTitleAndHall("%", "hall 8"))
                .extracting(ScreeningEntity::getId).containsExactly(screening.id());

        var user = users.save(new User(null, "  JpaUser" + System.nanoTime() + "@TEST.UA  ",
                "encoded", "JPA User", "ROLE_USER"));
        assertThat(user.email()).isEqualTo(user.email().trim().toLowerCase(java.util.Locale.ROOT));
        assertThat(userData.findByEmailNormalized("  " + user.email().toUpperCase() + " "))
                .isPresent().get().extracting(e -> e.getId()).isEqualTo(user.id());
        assertThatThrownBy(() -> users.save(new User(null, user.email().toUpperCase(),
                "another-hash", "Duplicate", "ROLE_USER")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");

        var ownedTicket = ticketService.book(orderedSeats.get(0).id(), user.id(), "Owned");
        var guestTicket = ticketService.book(orderedSeats.get(1).id(), "Guest");
        assertThat(guestTicket.userId()).isNull();
        assertThat(ticketData.findBySeat_Id(ownedTicket.seatId())).isPresent();
        assertThat(ticketData.findByUser_IdOrderByIdAsc(user.id()))
                .extracting(e -> e.getId()).containsExactly(ownedTicket.id());
        assertThat(ticketData.findBySeat_IdInOrderByIdAsc(
                java.util.List.of(ownedTicket.seatId(), guestTicket.seatId())))
                .extracting(e -> e.getId()).containsExactly(ownedTicket.id(), guestTicket.id());

        assertThatThrownBy(() -> tickets.save(new Ticket(null, guestTicket.seatId(), null, "Second guest",
                guestTicket.bookedAt())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat already has a ticket");
        assertThat(ticketData.findBySeat_Id(guestTicket.seatId())).hasValueSatisfying(
                entity -> assertThat(entity.getUser()).isNull());

        screenings.deleteById(screening.id());
        assertThat(screenings.findById(screening.id())).isEmpty();
        screeningService.delete(seatsForScreening.id());
    }

    @Test
    void serviceCreateCommitsOneScreeningAndExactlyFifteenSeats() {
        String title = "JpaCreate" + System.nanoTime();
        int screeningCountBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM screenings WHERE movie_title = ?", Integer.class, title);
        int seatCountBefore = jdbc.queryForObject("SELECT COUNT(*) FROM seats", Integer.class);

        Screening created = screeningService.create(title, "Description", 92,
                LocalDateTime.of(2027, 2, 1, 18, 0), "Hall 10", new BigDecimal("99.00"));

        assertThat(created.id()).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM screenings WHERE id = ? AND movie_title = ?",
                Integer.class, created.id(), title)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM seats WHERE screening_id = ?",
                Integer.class, created.id())).isEqualTo(15);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM screenings WHERE movie_title = ?",
                Integer.class, title)).isEqualTo(screeningCountBefore + 1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM seats", Integer.class)).isEqualTo(seatCountBefore + 15);

        screeningService.delete(created.id());
    }

    @Test
    void duplicateBookingIsRejectedAndScreeningDeleteRemovesLinkedRows() {
        Screening screening = screeningService.create("JpaDelete" + System.nanoTime(), "Description", 90,
                LocalDateTime.of(2027, 2, 2, 18, 0), "Hall 11", BigDecimal.TEN);
        var seat = screeningService.findSeats(screening.id()).getFirst();
        Ticket first = ticketService.book(seat.id(), "Guest ticket");
        assertThat(first.userId()).isNull();
        assertThatThrownBy(() -> ticketService.book(seat.id(), "Duplicate guest"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat already has a ticket");

        screeningService.delete(screening.id());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM screenings WHERE id = ?",
                Integer.class, screening.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM seats WHERE screening_id = ?",
                Integer.class, screening.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE id = ?",
                Integer.class, first.id())).isZero();
    }

}
