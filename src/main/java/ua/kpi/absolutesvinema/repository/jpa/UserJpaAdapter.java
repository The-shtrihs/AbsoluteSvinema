package ua.kpi.absolutesvinema.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import ua.kpi.absolutesvinema.model.User;
import ua.kpi.absolutesvinema.repository.UserRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.UserEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.UserJpaRepository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
@Profile("jpa")
public class UserJpaAdapter implements UserRepository {
    private final UserJpaRepository repository;

    public UserJpaAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "User is required");
        String email = normalize(user.email());
        if (user.id() != null && !repository.existsById(user.id())) {
            throw new IllegalArgumentException("Unknown user ID: " + user.id());
        }
        UserEntity entity = new UserEntity(user.id(), email, user.password(), user.fullName(), user.role());
        try {
            return toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException duplicate) {
            throw new IllegalStateException("User with this email already exists: " + user.email(), duplicate);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserJpaAdapter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return repository.findByEmail(normalize(email)).map(UserJpaAdapter::toDomain);
    }

    @Override
    public List<User> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(UserJpaAdapter::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getPassword(), entity.getFullName(), entity.getRole());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
