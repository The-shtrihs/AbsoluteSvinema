package ua.kpi.absolutesvinema.repository;

import ua.kpi.absolutesvinema.model.Ticket;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    /**
     * Creates when ID is null; otherwise updates an existing ticket.
     * Updates cannot change the seat. Unknown IDs cause IllegalArgumentException.
     * Checking seat availability and saving must be atomic: an occupied seat
     * causes IllegalStateException, including concurrent attempts to book it.
     */
    Ticket save(Ticket ticket);

    Optional<Ticket> findById(Long id);

    List<Ticket> findAll();

    /** Deleting a missing ID has no effect. */
    void deleteById(Long id);

    Optional<Ticket> findBySeatId(Long seatId);

    List<Ticket> findBySeatIdIn(Collection<Long> seatIds);
}
