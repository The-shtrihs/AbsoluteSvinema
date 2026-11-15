package ua.kpi.absolutesvinema.repository;

import ua.kpi.absolutesvinema.model.Ticket;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(Long id);

    List<Ticket> findAll();

    void deleteById(Long id);

    Optional<Ticket> findBySeatId(Long seatId);

    List<Ticket> findBySeatIdIn(Collection<Long> seatIds);

    List<Ticket> findByUserId(Long userId);
}
