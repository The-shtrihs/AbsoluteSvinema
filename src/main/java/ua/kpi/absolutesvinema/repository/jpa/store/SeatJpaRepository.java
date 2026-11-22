package ua.kpi.absolutesvinema.repository.jpa.store;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.SeatEntity;

import java.util.List;

public interface SeatJpaRepository extends JpaRepository<SeatEntity, Long> {
    List<SeatEntity> findAllByOrderByIdAsc();

    List<SeatEntity> findByScreening_IdOrderByRowNumberAscSeatNumberAsc(Long screeningId);
}
