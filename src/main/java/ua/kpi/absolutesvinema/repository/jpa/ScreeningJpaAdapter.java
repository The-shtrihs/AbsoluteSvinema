package ua.kpi.absolutesvinema.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.ScreeningJpaRepository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile("jpa")
public class ScreeningJpaAdapter implements ScreeningRepository {
    private final ScreeningJpaRepository repository;

    public ScreeningJpaAdapter(ScreeningJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Screening save(Screening screening) {
        Objects.requireNonNull(screening, "Screening is required");
        if (screening.id() != null && !repository.existsById(screening.id())) {
            throw new IllegalArgumentException("Unknown screening ID: " + screening.id());
        }
        ScreeningEntity entity = new ScreeningEntity(screening.id(), screening.movieTitle(),
                screening.movieDescription(), screening.movieDurationMinutes(), screening.startsAt(),
                screening.hall(), screening.ticketPrice());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Screening> findById(Long id) {
        return repository.findById(id).map(ScreeningJpaAdapter::toDomain);
    }

    @Override
    public List<Screening> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(ScreeningJpaAdapter::toDomain).toList();
    }

    @Override
    public List<Screening> findByTitleAndHall(String title, String hall) {
        String normalizedTitle = title == null || title.isBlank()
                ? null : title.trim().toLowerCase(Locale.ROOT);
        String normalizedHall = hall == null || hall.isBlank() ? null : hall.trim().toLowerCase(Locale.ROOT);
        return repository.search(normalizedTitle, normalizedHall).stream()
                .map(ScreeningJpaAdapter::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static Screening toDomain(ScreeningEntity entity) {
        return new Screening(entity.getId(), entity.getMovieTitle(), entity.getMovieDescription(),
                entity.getMovieDurationMinutes(), entity.getStartsAt(), entity.getHall(), entity.getTicketPrice());
    }

}
