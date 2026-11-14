package ua.kpi.absolutesvinema.repository;

import ua.kpi.absolutesvinema.model.Seat;

import java.util.List;
import java.util.Optional;

public interface SeatRepository {

    Seat save(Seat seat);

    Optional<Seat> findById(Long id);

    List<Seat> findAll();

    void deleteById(Long id);

    List<Seat> findByScreeningId(Long screeningId);
}
