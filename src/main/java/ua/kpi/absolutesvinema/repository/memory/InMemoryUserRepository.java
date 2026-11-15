package ua.kpi.absolutesvinema.repository.memory;

import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.User;
import ua.kpi.absolutesvinema.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized User save(User entity) {
        Objects.requireNonNull(entity, "User is required");
        if (entity.id() != null && !users.containsKey(entity.id())) {
            throw new IllegalArgumentException("Unknown user ID: " + entity.id());
        }
        Long id = entity.id() == null ? nextId++ : entity.id();
        User saved = new User(id, entity.email(), entity.password(), entity.fullName(), entity.role());
        users.put(id, saved);
        return saved;
    }

    @Override
    public synchronized Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public synchronized Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return users.values().stream()
                .filter(u -> u.email().equalsIgnoreCase(email.trim()))
                .findFirst();
    }

    @Override
    public synchronized List<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public synchronized void deleteById(Long id) {
        users.remove(id);
    }
}
