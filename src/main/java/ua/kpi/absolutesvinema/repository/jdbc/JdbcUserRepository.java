package ua.kpi.absolutesvinema.repository.jdbc;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.User;
import ua.kpi.absolutesvinema.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile("!memory")
public class JdbcUserRepository implements UserRepository {

    private static final String COLUMNS = "id, email, password, full_name, role";

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password"),
            rs.getString("full_name"),
            rs.getString("role"));

    private final JdbcClient jdbcClient;

    public JdbcUserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "User is required");
        try {
            return user.id() == null ? insert(user) : update(user);
        } catch (DuplicateKeyException duplicate) {
            throw new IllegalStateException("User with this email already exists: " + user.email(), duplicate);
        }
    }

    private User insert(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql("""
                        INSERT INTO users (email, password, full_name, role)
                        VALUES (:email, :password, :fullName, :role)
                        """)
                .param("email", normalize(user.email()))
                .param("password", user.password())
                .param("fullName", user.fullName())
                .param("role", user.role())
                .update(keyHolder, "id");
        long id = Objects.requireNonNull(keyHolder.getKey(), "No generated key returned").longValue();
        return new User(id, normalize(user.email()), user.password(), user.fullName(), user.role());
    }

    private User update(User user) {
        int updated = jdbcClient.sql("""
                        UPDATE users
                        SET email = :email, password = :password, full_name = :fullName, role = :role
                        WHERE id = :id
                        """)
                .param("email", normalize(user.email()))
                .param("password", user.password())
                .param("fullName", user.fullName())
                .param("role", user.role())
                .param("id", user.id())
                .update();
        if (updated == 0) {
            throw new IllegalArgumentException("Unknown user ID: " + user.id());
        }
        return new User(user.id(), normalize(user.email()), user.password(), user.fullName(), user.role());
    }

    @Override
    public Optional<User> findById(Long id) {
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM users WHERE id = :id")
                .param("id", id)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM users WHERE email = :email")
                .param("email", normalize(email))
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public List<User> findAll() {
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM users ORDER BY id")
                .query(ROW_MAPPER)
                .list();
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql("DELETE FROM users WHERE id = :id")
                .param("id", id)
                .update();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
