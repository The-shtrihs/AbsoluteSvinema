package ua.kpi.absolutesvinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** An immutable value; a null ID represents a new, unsaved entity. */
public record Screening(Long id, String movieTitle, String movieDescription, int movieDurationMinutes,
                        LocalDateTime startsAt, String hall, BigDecimal ticketPrice) {
    public Screening {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("ID must be positive or null");
        }
        if (movieTitle == null || movieTitle.isBlank()) {
            throw new IllegalArgumentException("Movie title must not be blank");
        }
        if (movieDescription == null || movieDescription.isBlank()) {
            throw new IllegalArgumentException("Movie description must not be blank");
        }
        if (movieDurationMinutes <= 0) {
            throw new IllegalArgumentException("Movie duration must be positive");
        }
        if (startsAt == null) {
            throw new IllegalArgumentException("Start time is required");
        }
        if (hall == null || hall.isBlank()) {
            throw new IllegalArgumentException("Hall must not be blank");
        }
        if (ticketPrice == null || ticketPrice.signum() < 0) {
            throw new IllegalArgumentException("Ticket price must be non-negative");
        }
    }
}
