package ua.kpi.absolutesvinema.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payload for creating or updating a cinema screening")
public record ScreeningRequest(
        @Schema(description = "Title of the movie", example = "Interstellar", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Movie title must not be blank")
        String movieTitle,

        @Schema(description = "Synopsis or description of the movie", example = "A team of explorers travel through a wormhole in space.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Movie description must not be blank")
        String movieDescription,

        @Schema(description = "Movie duration in minutes (positive integer)", example = "169", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Movie duration is required")
        @Positive(message = "Movie duration must be positive")
        Integer movieDurationMinutes,

        @Schema(description = "Screening start timestamp in ISO-8601 format", example = "2028-05-15T19:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Start time is required")
        LocalDateTime startsAt,

        @Schema(description = "Cinema hall name or number", example = "Hall 1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Hall must not be blank")
        String hall,

        @Schema(description = "Ticket price in UAH (non-negative)", example = "250.00", minimum = "0.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Ticket price is required")
        @DecimalMin(value = "0.0", message = "Ticket price must be non-negative")
        BigDecimal ticketPrice
) {
}
