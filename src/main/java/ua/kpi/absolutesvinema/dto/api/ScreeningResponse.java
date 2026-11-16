package ua.kpi.absolutesvinema.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import ua.kpi.absolutesvinema.model.Screening;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Representation of a cinema screening resource")
public record ScreeningResponse(
        @Schema(description = "Unique screening identifier", example = "1")
        Long id,

        @Schema(description = "Title of the movie", example = "Interstellar")
        String movieTitle,

        @Schema(description = "Synopsis or description of the movie", example = "A team of explorers travel through a wormhole in space.")
        String movieDescription,

        @Schema(description = "Movie duration in minutes", example = "169")
        Integer movieDurationMinutes,

        @Schema(description = "Screening start timestamp in ISO-8601 format", example = "2028-05-15T19:30:00")
        LocalDateTime startsAt,

        @Schema(description = "Cinema hall name or number", example = "Hall 1")
        String hall,

        @Schema(description = "Ticket price in UAH", example = "250.00")
        BigDecimal ticketPrice
) {
    public static ScreeningResponse from(Screening screening) {
        if (screening == null) {
            return null;
        }
        return new ScreeningResponse(
                screening.id(),
                screening.movieTitle(),
                screening.movieDescription(),
                screening.movieDurationMinutes(),
                screening.startsAt(),
                screening.hall(),
                screening.ticketPrice()
        );
    }
}
