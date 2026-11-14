package ua.kpi.absolutesvinema.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ModelValidationTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 12, 1, 18, 0);

    @ParameterizedTest
    @MethodSource("invalidModels")
    void rejectsInvalidData(Runnable constructor) {
        assertThatThrownBy(constructor::run).isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Runnable> invalidModels() {
        return Stream.of(
                () -> new Screening(null, "Film", null, 120, TIME, "Hall", BigDecimal.TEN),
                () -> new Screening(null, "Film", "", 120, TIME, "Hall", BigDecimal.TEN),
                () -> new Screening(null, "Film", "  ", 120, TIME, "Hall", BigDecimal.TEN),
                () -> new Screening(null, "Film", "Description", 0, TIME, "Hall", BigDecimal.TEN),
                () -> new Screening(null, "Film", "Description", -1, TIME, "Hall", BigDecimal.TEN),
                () -> new Screening(0L, "Film", "A short film description", 120, TIME, "Hall", BigDecimal.TEN),
                () -> new Seat(0L, 1L, 1, 1),
                () -> new Ticket(0L, 1L, "Guest", TIME),
                () -> new Screening(-1L, "Film", "A short film description", 120, TIME, "Hall", BigDecimal.TEN),
                () -> new Seat(-1L, 1L, 1, 1),
                () -> new Ticket(-1L, 1L, "Guest", TIME),
                () -> new Screening(null, null, "A short film description", 120, TIME, "Hall", BigDecimal.TEN),
                () -> new Screening(null, "Film", "A short film description", 120, TIME, null, BigDecimal.TEN),
                () -> new Ticket(null, 1L, null, TIME),
                () -> new Screening(null, "", "A short film description", 120, TIME, "Hall", BigDecimal.TEN),
                () -> new Screening(null, "Film", "A short film description", 120, TIME, "", BigDecimal.TEN),
                () -> new Ticket(null, 1L, "", TIME),
                () -> new Screening(null, "  ", "A short film description", 120, TIME, "Hall", BigDecimal.TEN),
                () -> new Screening(null, "Film", "A short film description", 120, TIME, "  ", BigDecimal.TEN),
                () -> new Ticket(null, 1L, "  ", TIME),
                () -> new Seat(null, null, 1, 1),
                () -> new Ticket(null, null, "Guest", TIME),
                () -> new Seat(null, 0L, 1, 1),
                () -> new Ticket(null, 0L, "Guest", TIME),
                () -> new Seat(null, -1L, 1, 1),
                () -> new Ticket(null, -1L, "Guest", TIME),
                () -> new Screening(null, "Film", "A short film description", 120, null, "Hall", BigDecimal.TEN),
                () -> new Screening(null, "Film", "A short film description", 120, TIME, "Hall", null),
                () -> new Screening(null, "Film", "A short film description", 120, TIME, "Hall", BigDecimal.valueOf(-1)),
                () -> new Ticket(null, 1L, "Guest", null),
                () -> new Seat(null, 1L, 0, 1),
                () -> new Seat(null, 1L, -1, 1),
                () -> new Seat(null, 1L, 1, 0),
                () -> new Seat(null, 1L, 1, -1));
    }

    @Test
    void createsScreeningWithDescriptionAndDuration() {
        var screening = new Screening(null, "Film", "A short description", 1, TIME, "Hall", BigDecimal.TEN);
        assertThat(screening.movieDescription()).isEqualTo("A short description");
        assertThat(screening.movieDurationMinutes()).isEqualTo(1);
    }

    @Test
    void acceptsUnsavedModelsAndZeroPrice() {
        assertThat(new Screening(null, "Film", "A short film description", 120, TIME, "Hall", BigDecimal.ZERO).id()).isNull();
        assertThat(new Seat(null, 1L, 1, 1).id()).isNull();
        assertThat(new Ticket(null, 1L, "Guest", TIME).id()).isNull();
    }
}
