package ua.kpi.absolutesvinema.repository.jpa;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.repository.TicketRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.SeatEntity;
import ua.kpi.absolutesvinema.repository.jpa.entity.TicketEntity;
import ua.kpi.absolutesvinema.repository.jpa.entity.UserEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.TicketJpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile("jpa")
public class TicketJpaAdapter implements TicketRepository {
    private final TicketJpaRepository repository;
    private final EntityManager entityManager;

    public TicketJpaAdapter(TicketJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Ticket save(Ticket ticket) {
        Objects.requireNonNull(ticket, "Ticket is required");
        if (ticket.id() != null) {
            TicketEntity existing = repository.findById(ticket.id())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown ticket ID: " + ticket.id()));
            if (!existing.getSeat().getId().equals(ticket.seatId())) {
                throw new IllegalArgumentException("A ticket cannot be moved to another seat");
            }
        }
        SeatEntity seat = entityManager.getReference(SeatEntity.class, ticket.seatId());
        UserEntity user = ticket.userId() == null ? null
                : entityManager.getReference(UserEntity.class, ticket.userId());
        TicketEntity entity = new TicketEntity(ticket.id(), seat, user, ticket.customerName(), ticket.bookedAt());
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return repository.findById(id).map(TicketJpaAdapter::toDomain);
    }

    @Override
    public List<Ticket> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(TicketJpaAdapter::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<Ticket> findBySeatId(Long seatId) {
        return repository.findBySeat_Id(seatId).map(TicketJpaAdapter::toDomain);
    }

    @Override
    public List<Ticket> findBySeatIdIn(Collection<Long> seatIds) {
        Objects.requireNonNull(seatIds, "Seat IDs are required");
        if (seatIds.isEmpty()) {
            return List.of();
        }
        return repository.findBySeat_IdInOrderByIdAsc(seatIds).stream().map(TicketJpaAdapter::toDomain).toList();
    }

    @Override
    public List<Ticket> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return repository.findByUser_IdOrderByIdAsc(userId).stream().map(TicketJpaAdapter::toDomain).toList();
    }

    private static Ticket toDomain(TicketEntity entity) {
        return new Ticket(entity.getId(), entity.getSeat().getId(),
                entity.getUser() == null ? null : entity.getUser().getId(),
                entity.getCustomerName(), entity.getBookedAt());
    }
}
