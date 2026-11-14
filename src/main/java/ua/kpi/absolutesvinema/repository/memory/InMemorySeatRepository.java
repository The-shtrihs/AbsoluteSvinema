package ua.kpi.absolutesvinema.repository.memory;

import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.repository.SeatRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class InMemorySeatRepository implements SeatRepository {
    private final Map<Long, Seat> seats = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized Seat save(Seat entity) {
        Objects.requireNonNull(entity, "Seat is required");
        if (entity.id() != null && !seats.containsKey(entity.id())) {
            throw new IllegalArgumentException("Unknown seat ID: " + entity.id());
        }
        Long id = entity.id() == null ? nextId++ : entity.id();
        Seat saved = new Seat(id, entity.screeningId(), entity.rowNumber(), entity.seatNumber());
        seats.put(id, saved);
        return saved;
    }

    @Override
    public synchronized Optional<Seat> findById(Long id) {
        return Optional.ofNullable(seats.get(id));
    }

    @Override
    public synchronized List<Seat> findAll() {
        return List.copyOf(seats.values());
    }

    @Override
    public synchronized void deleteById(Long id) {
        seats.remove(id);
    }

    @Override
    public synchronized List<Seat> findByScreeningId(Long screeningId) {
        return seats.values().stream()
                .filter(seat -> seat.screeningId().equals(screeningId))
                .toList();
    }
}
