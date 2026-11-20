package ua.kpi.absolutesvinema.repository.jdbc;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.repository.SeatRepository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile("!memory & !jpa")
public class JdbcSeatRepository implements SeatRepository {

    private static final String COLUMNS = "id, screening_id, row_number, seat_number";

    private static final RowMapper<Seat> ROW_MAPPER = (rs, rowNum) -> new Seat(
            rs.getLong("id"),
            rs.getLong("screening_id"),
            rs.getInt("row_number"),
            rs.getInt("seat_number"));

    private final JdbcTemplate jdbcTemplate;

    public JdbcSeatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Seat save(Seat seat) {
        Objects.requireNonNull(seat, "Seat is required");
        if (seat.id() == null) {
            return insert(seat);
        }
        int updated = jdbcTemplate.update(
                "UPDATE seats SET screening_id = ?, row_number = ?, seat_number = ? WHERE id = ?",
                seat.screeningId(), seat.rowNumber(), seat.seatNumber(), seat.id());
        if (updated == 0) {
            throw new IllegalArgumentException("Unknown seat ID: " + seat.id());
        }
        return seat;
    }

    private Seat insert(Seat seat) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO seats (screening_id, row_number, seat_number) VALUES (?, ?, ?)",
                    new String[]{"id"});
            ps.setLong(1, seat.screeningId());
            ps.setInt(2, seat.rowNumber());
            ps.setInt(3, seat.seatNumber());
            return ps;
        }, keyHolder);
        long id = Objects.requireNonNull(keyHolder.getKey(), "No generated key returned").longValue();
        return new Seat(id, seat.screeningId(), seat.rowNumber(), seat.seatNumber());
    }

    @Override
    public Optional<Seat> findById(Long id) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM seats WHERE id = ?", ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    @Override
    public List<Seat> findAll() {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM seats ORDER BY id", ROW_MAPPER);
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM seats WHERE id = ?", id);
    }

    @Override
    public List<Seat> findByScreeningId(Long screeningId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM seats WHERE screening_id = ? ORDER BY row_number, seat_number",
                ROW_MAPPER, screeningId);
    }
}
