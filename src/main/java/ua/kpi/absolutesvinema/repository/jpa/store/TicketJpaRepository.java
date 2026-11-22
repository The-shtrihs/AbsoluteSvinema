package ua.kpi.absolutesvinema.repository.jpa.store;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.TicketEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long> {
    List<TicketEntity> findAllByOrderByIdAsc();

    Optional<TicketEntity> findBySeat_Id(Long seatId);

    List<TicketEntity> findBySeat_IdInOrderByIdAsc(Collection<Long> seatIds);

    List<TicketEntity> findByUser_IdOrderByIdAsc(Long userId);
}
