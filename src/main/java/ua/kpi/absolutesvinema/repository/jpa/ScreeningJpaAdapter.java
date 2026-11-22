package ua.kpi.absolutesvinema.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.ScreeningJpaDataRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Repository
@Profile("jpa")
public class ScreeningJpaAdapter implements ScreeningRepository {
    private final ScreeningJpaDataRepository repository;
    private final EntityManager entityManager;

    public ScreeningJpaAdapter(ScreeningJpaDataRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Screening save(Screening screening) {
        Objects.requireNonNull(screening, "Screening is required");
        if (screening.id() != null && !repository.existsById(screening.id())) {
            throw new IllegalArgumentException("Unknown screening ID: " + screening.id());
        }
        ScreeningEntity entity = new ScreeningEntity(screening.id(), screening.movieTitle(),
                screening.movieDescription(), screening.movieDurationMinutes(), screening.startsAt(),
                screening.hall(), screening.ticketPrice());
        ScreeningEntity saved = repository.save(entity);
        entityManager.flush();
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Screening> findById(Long id) {
        return repository.findById(id).map(ScreeningJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Screening> findAll() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(ScreeningEntity::getId))
                .map(ScreeningJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Screening> findByTitleAndHall(String title, String hall) {
        return repository.findByTitleAndHall(title, hall).stream()
                .map(ScreeningJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static Screening toDomain(ScreeningEntity entity) {
        return new Screening(entity.getId(), entity.getMovieTitle(), entity.getMovieDescription(),
                entity.getMovieDurationMinutes(), entity.getStartsAt(), entity.getHall(), entity.getTicketPrice());
    }
}
