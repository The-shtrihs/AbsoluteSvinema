package ua.kpi.absolutesvinema.repository.jdbc;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.repository.TicketRepository;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile("!memory & !jpa")
public class JdbcTicketRepository implements TicketRepository {

    private static final String COLUMNS = "id, seat_id, user_id, customer_name, booked_at";

    private static final RowMapper<Ticket> ROW_MAPPER = (rs, rowNum) -> new Ticket(
            rs.getLong("id"),
            rs.getLong("seat_id"),
            rs.getObject("user_id", Long.class),
            rs.getString("customer_name"),
            rs.getObject("booked_at", LocalDateTime.class));

    private final JdbcClient jdbcClient;

    public JdbcTicketRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Ticket save(Ticket ticket) {
        Objects.requireNonNull(ticket, "Ticket is required");
        Long id = ticket.id() == null ? insert(ticket) : update(ticket);
        return findById(id).orElseThrow();
    }

    private Long insert(Ticket ticket) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcClient.sql("""
                            INSERT INTO tickets (seat_id, user_id, customer_name, booked_at)
                            VALUES (:seatId, :userId, :customerName, :bookedAt)
                            """)
                    .param("seatId", ticket.seatId())
                    .param("userId", ticket.userId(), Types.BIGINT)
                    .param("customerName", ticket.customerName())
                    .param("bookedAt", ticket.bookedAt())
                    .update(keyHolder, "id");
        } catch (DuplicateKeyException occupied) {
            throw new IllegalStateException("Seat already has a ticket: " + ticket.seatId(), occupied);
        }
        return Objects.requireNonNull(keyHolder.getKey(), "No generated key returned").longValue();
    }

    private Long update(Ticket ticket) {
        Ticket existing = findById(ticket.id())
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticket ID: " + ticket.id()));
        if (!existing.seatId().equals(ticket.seatId())) {
            throw new IllegalArgumentException("A ticket cannot be moved to another seat");
        }
        jdbcClient.sql("""
                        UPDATE tickets
                        SET user_id = :userId, customer_name = :customerName, booked_at = :bookedAt
                        WHERE id = :id
                        """)
                .param("userId", ticket.userId(), Types.BIGINT)
                .param("customerName", ticket.customerName())
                .param("bookedAt", ticket.bookedAt())
                .param("id", ticket.id())
                .update();
        return ticket.id();
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM tickets WHERE id = :id")
                .param("id", id)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public List<Ticket> findAll() {
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM tickets ORDER BY id")
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql("DELETE FROM tickets WHERE id = :id")
                .param("id", id)
                .update();
    }

    @Override
    public Optional<Ticket> findBySeatId(Long seatId) {
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM tickets WHERE seat_id = :seatId")
                .param("seatId", seatId)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public List<Ticket> findBySeatIdIn(Collection<Long> seatIds) {
        Objects.requireNonNull(seatIds, "Seat IDs are required");
        if (seatIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM tickets WHERE seat_id IN (:seatIds) ORDER BY id")
                .param("seatIds", seatIds)
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public List<Ticket> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM tickets WHERE user_id = :userId ORDER BY id")
                .param("userId", userId)
                .query(ROW_MAPPER)
                .list();
    }
}
