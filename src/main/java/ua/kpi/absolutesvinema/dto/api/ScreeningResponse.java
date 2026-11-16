package ua.kpi.absolutesvinema.dto.api;

import ua.kpi.absolutesvinema.model.Screening;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScreeningResponse(
        Long id,
        String movieTitle,
        String movieDescription,
        Integer movieDurationMinutes,
        LocalDateTime startsAt,
        String hall,
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
