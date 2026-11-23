package ua.kpi.absolutesvinema.repository.jpa.store;

import org.springframework.data.repository.CrudRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.SeatEntity;

import java.util.List;

public interface SeatJpaDataRepository extends CrudRepository<SeatEntity, Long> {
    List<SeatEntity> findByScreening_IdOrderByRowNumberAscSeatNumberAsc(Long screeningId);
}
