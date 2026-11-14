package ua.kpi.absolutesvinema.model;

/** An immutable value; a null ID represents a new, unsaved entity. */
public record Seat(Long id, Long screeningId, int rowNumber, int seatNumber) {
    public Seat {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("ID must be positive or null");
        }
        if (screeningId == null || screeningId <= 0) {
            throw new IllegalArgumentException("Screening ID must be positive");
        }
        if (rowNumber <= 0 || seatNumber <= 0) {
            throw new IllegalArgumentException("Row and seat numbers must be positive");
        }
    }
}
