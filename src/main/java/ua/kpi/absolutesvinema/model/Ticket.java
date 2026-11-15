package ua.kpi.absolutesvinema.model;

import java.time.LocalDateTime;

public record Ticket(Long id, Long seatId, Long userId, String customerName, LocalDateTime bookedAt) {
    public Ticket(Long id, Long seatId, String customerName, LocalDateTime bookedAt) {
        this(id, seatId, null, customerName, bookedAt);
    }

    public Ticket {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("ID must be positive or null");
        }
        if (seatId == null || seatId <= 0) {
            throw new IllegalArgumentException("Seat ID must be positive");
        }
        if (userId != null && userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be blank");
        }
        if (bookedAt == null) {
            throw new IllegalArgumentException("Booking time is required");
        }
    }
}
