package ua.kpi.absolutesvinema.repository.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class JdbcScreeningSeatRepositoryTest {

    private static final LocalDateTime STARTS_AT = LocalDateTime.of(2027, 1, 10, 18, 30);

    @Autowired
    private ScreeningRepository screenings;

    @Autowired
    private SeatRepository seats;

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void jdbcRepositoriesAreActive() {
        assertThat(screenings).isInstanceOf(JdbcScreeningRepository.class);
        assertThat(seats).isInstanceOf(JdbcSeatRepository.class);
    }

    @Test
    void screeningCrudUsesGeneratedIdAndRejectsUnknownUpdate() {
        Screening saved = screenings.save(new Screening(null, "Jdbc Crud Film", "Description", 100,
                STARTS_AT, "Hall 9", new BigDecimal("150.00")));

        assertThat(saved.id()).isPositive();
        assertThat(screenings.findById(saved.id())).contains(saved);

        Screening updated = screenings.save(new Screening(saved.id(), "Jdbc Crud Film 2", "Changed", 110,
                STARTS_AT.plusHours(1), "Hall 8", new BigDecimal("175.50")));
        assertThat(screenings.findById(saved.id())).contains(updated);
        assertThat(updated.ticketPrice()).isEqualByComparingTo("175.50");

        assertThatThrownBy(() -> screenings.save(new Screening(999_999L, "Ghost", "Ghost", 90,
                STARTS_AT, "Hall 1", BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(screenings.findById(999_999L)).isEmpty();

        screenings.deleteById(saved.id());
        assertThat(screenings.findById(saved.id())).isEmpty();
    }

    @Test
    void searchFiltersByTitleSubstringAndHallIgnoringCase() {
        String suffix = "_" + System.nanoTime();
        Screening a = screenings.save(new Screening(null, "Search Alpha" + suffix, "D", 90, STARTS_AT, "Hall 5", BigDecimal.TEN));
        Screening b = screenings.save(new Screening(null, "Search Beta" + suffix, "D", 90, STARTS_AT, "Hall 6", BigDecimal.TEN));

        assertThat(screenings.findByTitleAndHall("search", null)).contains(a, b);
        assertThat(screenings.findByTitleAndHall("ALPHA" + suffix, null)).containsExactly(a);
        assertThat(screenings.findByTitleAndHall("search", " hall 6 ")).containsExactly(b);
        assertThat(screenings.findByTitleAndHall("%", null)).doesNotContain(a, b);
    }

    @Test
    void createStoresExactlyFifteenOrderedSeatsAndDeleteRemovesThem() {
        Screening screening = screeningService.create("Seat Film", "Description", 95, STARTS_AT,
                "Hall 7", new BigDecimal("120.00"));

        var stored = seats.findByScreeningId(screening.id());
        assertThat(stored).hasSize(15);
        assertThat(stored.getFirst()).extracting(Seat::rowNumber, Seat::seatNumber).containsExactly(1, 1);
        assertThat(stored.getLast()).extracting(Seat::rowNumber, Seat::seatNumber).containsExactly(3, 5);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM seats WHERE screening_id = ?",
                Integer.class, screening.id())).isEqualTo(15);

        screeningService.delete(screening.id());

        assertThat(screenings.findById(screening.id())).isEmpty();
        assertThat(seats.findByScreeningId(screening.id())).isEmpty();
    }
}
