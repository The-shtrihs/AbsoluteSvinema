package ua.kpi.absolutesvinema.repository.jdbc;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * ScreeningRepository on PostgreSQL via JdbcTemplate: RowMapper for reads,
 * GeneratedKeyHolder for the database-generated ID, only bound parameters in SQL.
 */
@Repository
@Profile("!memory")
public class JdbcScreeningRepository implements ScreeningRepository {

    private static final String COLUMNS =
            "id, movie_title, movie_description, movie_duration_minutes, starts_at, hall, ticket_price";

    private static final RowMapper<Screening> ROW_MAPPER = (rs, rowNum) -> new Screening(
            rs.getLong("id"),
            rs.getString("movie_title"),
            rs.getString("movie_description"),
            rs.getInt("movie_duration_minutes"),
            rs.getObject("starts_at", LocalDateTime.class),
            rs.getString("hall"),
            rs.getBigDecimal("ticket_price"));

    private final JdbcTemplate jdbcTemplate;

    public JdbcScreeningRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Screening save(Screening screening) {
        Objects.requireNonNull(screening, "Screening is required");
        Long id = screening.id() == null ? insert(screening) : update(screening);
        // Re-read so the result has exactly what PostgreSQL stored (NUMERIC scale, TIMESTAMP precision).
        return findById(id).orElseThrow();
    }

    private Long insert(Screening screening) {
        String sql = """
                INSERT INTO screenings (movie_title, movie_description, movie_duration_minutes, starts_at, hall, ticket_price)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, screening.movieTitle());
            ps.setString(2, screening.movieDescription());
            ps.setInt(3, screening.movieDurationMinutes());
            ps.setTimestamp(4, Timestamp.valueOf(screening.startsAt()));
            ps.setString(5, screening.hall());
            ps.setBigDecimal(6, screening.ticketPrice());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey(), "No generated key returned").longValue();
    }

    private Long update(Screening screening) {
        int updated = jdbcTemplate.update("""
                        UPDATE screenings
                        SET movie_title = ?, movie_description = ?, movie_duration_minutes = ?,
                            starts_at = ?, hall = ?, ticket_price = ?
                        WHERE id = ?
                        """,
                screening.movieTitle(), screening.movieDescription(), screening.movieDurationMinutes(),
                Timestamp.valueOf(screening.startsAt()), screening.hall(), screening.ticketPrice(),
                screening.id());
        if (updated == 0) {
            throw new IllegalArgumentException("Unknown screening ID: " + screening.id());
        }
        return screening.id();
    }

    @Override
    public Optional<Screening> findById(Long id) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM screenings WHERE id = ?", ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    @Override
    public List<Screening> findAll() {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM screenings ORDER BY id", ROW_MAPPER);
    }

    @Override
    public List<Screening> findByTitleAndHall(String title, String hall) {
        // Only fixed SQL fragments are appended; user input always goes through '?' parameters.
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM screenings WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (title != null && !title.isBlank()) {
            sql.append(" AND LOWER(movie_title) LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(title.trim().toLowerCase(Locale.ROOT)) + "%");
        }
        if (hall != null && !hall.isBlank()) {
            sql.append(" AND LOWER(TRIM(hall)) = ?");
            params.add(hall.trim().toLowerCase(Locale.ROOT));
        }
        sql.append(" ORDER BY id");
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM screenings WHERE id = ?", id);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
