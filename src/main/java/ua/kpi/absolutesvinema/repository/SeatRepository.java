package ua.kpi.absolutesvinema.repository;

import ua.kpi.absolutesvinema.model.Seat;

import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    /** Creates when ID is null; otherwise updates an existing entity or throws IllegalArgumentException. */
    Seat save(Seat seat);

    Optional<Seat> findById(Long id);

    List<Seat> findAll();

    /** Deleting a missing ID has no effect. */
    void deleteById(Long id);

    List<Seat> findByScreeningId(Long screeningId);
}
