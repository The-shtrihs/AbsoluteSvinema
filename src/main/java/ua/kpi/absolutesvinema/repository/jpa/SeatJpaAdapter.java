package ua.kpi.absolutesvinema.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;
import ua.kpi.absolutesvinema.repository.jpa.entity.SeatEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.SeatJpaRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile("jpa")
public class SeatJpaAdapter implements SeatRepository {
    private final SeatJpaRepository repository;
    private final EntityManager entityManager;

    public SeatJpaAdapter(SeatJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Seat save(Seat seat) {
        Objects.requireNonNull(seat, "Seat is required");
        if (seat.id() != null && !repository.existsById(seat.id())) {
            throw new IllegalArgumentException("Unknown seat ID: " + seat.id());
        }
        ScreeningEntity screening = entityManager.getReference(ScreeningEntity.class, seat.screeningId());
        SeatEntity entity = new SeatEntity(seat.id(), screening, seat.rowNumber(), seat.seatNumber());
        SeatEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Seat> findById(Long id) {
        return repository.findById(id).map(SeatJpaAdapter::toDomain);
    }

    @Override
    public List<Seat> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(SeatJpaAdapter::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<Seat> findByScreeningId(Long screeningId) {
        return repository.findByScreening_IdOrderByRowNumberAscSeatNumberAsc(screeningId).stream()
                .map(SeatJpaAdapter::toDomain).toList();
    }

    private static Seat toDomain(SeatEntity entity) {
        return new Seat(entity.getId(), entity.getScreening().getId(), entity.getRowNumber(), entity.getSeatNumber());
    }
}
