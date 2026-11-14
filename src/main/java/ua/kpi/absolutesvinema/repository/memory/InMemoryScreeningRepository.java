package ua.kpi.absolutesvinema.repository.memory;

import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class InMemoryScreeningRepository implements ScreeningRepository {
    private final Map<Long, Screening> screenings = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized Screening save(Screening entity) {
        Objects.requireNonNull(entity, "Screening is required");
        if (entity.id() != null && !screenings.containsKey(entity.id())) {
            throw new IllegalArgumentException("Unknown screening ID: " + entity.id());
        }
        Long id = entity.id() == null ? nextId++ : entity.id();
        Screening saved = new Screening(id, entity.movieTitle(), entity.startsAt(), entity.hall(), entity.ticketPrice());
        screenings.put(id, saved);
        return saved;
    }

    @Override
    public synchronized Optional<Screening> findById(Long id) {
        return Optional.ofNullable(screenings.get(id));
    }

    @Override
    public synchronized List<Screening> findAll() {
        return List.copyOf(screenings.values());
    }

    @Override
    public synchronized void deleteById(Long id) {
        screenings.remove(id);
    }
}
