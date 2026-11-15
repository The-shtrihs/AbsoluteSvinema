package ua.kpi.absolutesvinema.repository.memory;

import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.repository.TicketRepository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class InMemoryTicketRepository implements TicketRepository {
    private final Map<Long, Ticket> tickets = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized Ticket save(Ticket entity) {
        Objects.requireNonNull(entity, "Ticket is required");
        if (entity.id() != null && !tickets.containsKey(entity.id())) {
            throw new IllegalArgumentException("Unknown ticket ID: " + entity.id());
        }
        Ticket existing = tickets.get(entity.id());
        if (existing != null && !existing.seatId().equals(entity.seatId())) {
            throw new IllegalArgumentException("A ticket cannot be moved to another seat");
        }
        boolean occupied = tickets.values().stream()
                .anyMatch(ticket -> ticket.seatId().equals(entity.seatId())
                        && !ticket.id().equals(entity.id()));
        if (occupied) {
            throw new IllegalStateException("Seat already has a ticket: " + entity.seatId());
        }
        Long id = entity.id() == null ? nextId++ : entity.id();
        Ticket saved = new Ticket(id, entity.seatId(), entity.userId(), entity.customerName(), entity.bookedAt());
        tickets.put(id, saved);
        return saved;
    }

    @Override
    public synchronized Optional<Ticket> findById(Long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public synchronized List<Ticket> findAll() {
        return List.copyOf(tickets.values());
    }

    @Override
    public synchronized void deleteById(Long id) {
        tickets.remove(id);
    }

    @Override
    public synchronized Optional<Ticket> findBySeatId(Long seatId) {
        return tickets.values().stream()
                .filter(ticket -> ticket.seatId().equals(seatId))
                .findFirst();
    }

    @Override
    public synchronized List<Ticket> findBySeatIdIn(Collection<Long> seatIds) {
        Objects.requireNonNull(seatIds, "Seat IDs are required");
        return tickets.values().stream()
                .filter(ticket -> seatIds.contains(ticket.seatId()))
                .toList();
    }

    @Override
    public synchronized List<Ticket> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return tickets.values().stream()
                .filter(ticket -> Objects.equals(ticket.userId(), userId))
                .toList();
    }
}
