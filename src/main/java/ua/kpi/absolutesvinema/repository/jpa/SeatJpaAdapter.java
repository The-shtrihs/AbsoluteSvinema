package ua.kpi.absolutesvinema.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;
import ua.kpi.absolutesvinema.repository.jpa.entity.SeatEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.SeatJpaDataRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Repository
@Profile("jpa")
public class SeatJpaAdapter implements SeatRepository {
    private final SeatJpaDataRepository repository;
    private final EntityManager entityManager;

    public SeatJpaAdapter(SeatJpaDataRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public Seat save(Seat seat) {
        Objects.requireNonNull(seat, "Seat is required");
        if (seat.id() != null && !repository.existsById(seat.id())) {
            throw new IllegalArgumentException("Unknown seat ID: " + seat.id());
        }
        ScreeningEntity screening = entityManager.getReference(ScreeningEntity.class, seat.screeningId());
        SeatEntity saved = repository.save(new SeatEntity(seat.id(), screening, seat.rowNumber(), seat.seatNumber()));
        entityManager.flush();
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Seat> findById(Long id) {
        return repository.findById(id).map(SeatJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> findAll() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(SeatEntity::getId))
                .map(SeatJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> findByScreeningId(Long screeningId) {
        return repository.findByScreening_IdOrderByRowNumberAscSeatNumberAsc(screeningId).stream()
                .map(SeatJpaAdapter::toDomain)
                .toList();
    }

    private static Seat toDomain(SeatEntity entity) {
        return new Seat(entity.getId(), entity.getScreening().getId(), entity.getRowNumber(), entity.getSeatNumber());
    }
}
