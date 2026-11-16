package ua.kpi.absolutesvinema.dto.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScreeningRequest(
        @NotBlank(message = "Movie title must not be blank")
        String movieTitle,

        @NotBlank(message = "Movie description must not be blank")
        String movieDescription,

        @NotNull(message = "Movie duration is required")
        @Positive(message = "Movie duration must be positive")
        Integer movieDurationMinutes,

        @NotNull(message = "Start time is required")
        LocalDateTime startsAt,

        @NotBlank(message = "Hall must not be blank")
        String hall,

        @NotNull(message = "Ticket price is required")
        @DecimalMin(value = "0.0", message = "Ticket price must be non-negative")
        BigDecimal ticketPrice
) {
}
