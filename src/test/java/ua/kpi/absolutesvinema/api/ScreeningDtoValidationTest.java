package ua.kpi.absolutesvinema.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.kpi.absolutesvinema.dto.api.ScreeningRequest;
import ua.kpi.absolutesvinema.dto.api.ScreeningResponse;
import ua.kpi.absolutesvinema.model.Screening;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ScreeningDtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validScreeningRequestHasNoViolations() {
        ScreeningRequest request = new ScreeningRequest(
                "Inception",
                "A mind-bending thriller",
                148,
                LocalDateTime.of(2027, 5, 20, 19, 0),
                "Hall 1",
                new BigDecimal("150.00")
        );

        var violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsBlankOrNullFields() {
        ScreeningRequest request = new ScreeningRequest(
                "   ",
                null,
                null,
                null,
                "",
                null
        );

        var violations = validator.validate(request);
        assertThat(violations).hasSize(6);
        assertThat(violations).extracting("propertyPath").asString()
                .contains("movieTitle", "movieDescription", "movieDurationMinutes", "startsAt", "hall", "ticketPrice");
    }

    @Test
    void rejectsNonPositiveDuration() {
        ScreeningRequest zeroDuration = new ScreeningRequest(
                "Film", "Desc", 0, LocalDateTime.now().plusDays(1), "Hall A", BigDecimal.TEN);
        ScreeningRequest negativeDuration = new ScreeningRequest(
                "Film", "Desc", -10, LocalDateTime.now().plusDays(1), "Hall A", BigDecimal.TEN);

        assertThat(validator.validate(zeroDuration)).isNotEmpty();
        assertThat(validator.validate(negativeDuration)).isNotEmpty();
    }

    @Test
    void rejectsNegativeTicketPrice() {
        ScreeningRequest negativePrice = new ScreeningRequest(
                "Film", "Desc", 100, LocalDateTime.now().plusDays(1), "Hall A", new BigDecimal("-1.00"));

        assertThat(validator.validate(negativePrice)).isNotEmpty();
    }

    @Test
    void allowsZeroTicketPrice() {
        ScreeningRequest freeScreening = new ScreeningRequest(
                "Film", "Desc", 100, LocalDateTime.now().plusDays(1), "Hall A", BigDecimal.ZERO);

        assertThat(validator.validate(freeScreening)).isEmpty();
    }

    @Test
    void screeningResponseConversionFactoryMapsAllFields() {
        Screening screening = new Screening(
                42L,
                "Interstellar",
                "Space exploration",
                169,
                LocalDateTime.of(2027, 3, 15, 20, 0),
                "IMAX",
                new BigDecimal("220.50")
        );

        ScreeningResponse response = ScreeningResponse.from(screening);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.movieTitle()).isEqualTo("Interstellar");
        assertThat(response.movieDescription()).isEqualTo("Space exploration");
        assertThat(response.movieDurationMinutes()).isEqualTo(169);
        assertThat(response.startsAt()).isEqualTo(LocalDateTime.of(2027, 3, 15, 20, 0));
        assertThat(response.hall()).isEqualTo("IMAX");
        assertThat(response.ticketPrice()).isEqualByComparingTo("220.50");
    }

    @Test
    void screeningResponseFromNullReturnsNull() {
        assertThat(ScreeningResponse.from(null)).isNull();
    }
}
