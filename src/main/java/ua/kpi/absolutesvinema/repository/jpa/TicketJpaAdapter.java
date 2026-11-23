package ua.kpi.absolutesvinema.repository.jpa;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.repository.TicketRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.SeatEntity;
import ua.kpi.absolutesvinema.repository.jpa.entity.TicketEntity;
import ua.kpi.absolutesvinema.repository.jpa.entity.UserEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.TicketJpaDataRepository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Repository
@Profile("jpa")
public class TicketJpaAdapter implements TicketRepository {
    private final TicketJpaDataRepository repository;
    private final EntityManager entityManager;

    public TicketJpaAdapter(TicketJpaDataRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
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
        TicketEntity saved;
        try {
            saved = repository.save(new TicketEntity(ticket.id(), seat, user,
                    ticket.customerName(), ticket.bookedAt()));
            entityManager.flush();
        } catch (RuntimeException failure) {
            if (hasConstraint(failure, "uk_tickets_seat")) {
                throw new IllegalStateException("Seat already has a ticket: " + ticket.seatId(), failure);
            }
            throw failure;
        }
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findById(Long id) {
        return repository.findById(id).map(TicketJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findAll() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(TicketEntity::getId))
                .map(TicketJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ticket> findBySeatId(Long seatId) {
        return repository.findBySeat_Id(seatId).map(TicketJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findBySeatIdIn(Collection<Long> seatIds) {
        Objects.requireNonNull(seatIds, "Seat IDs are required");
        if (seatIds.isEmpty()) {
            return List.of();
        }
        return repository.findBySeat_IdInOrderByIdAsc(seatIds).stream()
                .map(TicketJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return repository.findByUser_IdOrderByIdAsc(userId).stream()
                .map(TicketJpaAdapter::toDomain)
                .toList();
    }

    private static Ticket toDomain(TicketEntity entity) {
        return new Ticket(entity.getId(), entity.getSeat().getId(),
                entity.getUser() == null ? null : entity.getUser().getId(),
                entity.getCustomerName(), entity.getBookedAt());
    }

    private static boolean hasConstraint(Throwable failure, String expectedName) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null
                    && violation.getConstraintName().toLowerCase(java.util.Locale.ROOT)
                    .contains(expectedName.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
