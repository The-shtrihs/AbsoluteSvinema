package ua.kpi.absolutesvinema.repository.jpa;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.absolutesvinema.model.User;
import ua.kpi.absolutesvinema.repository.UserRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.UserEntity;
import ua.kpi.absolutesvinema.repository.jpa.store.UserJpaDataRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Repository
@Profile("jpa")
public class UserJpaAdapter implements UserRepository {
    private final UserJpaDataRepository repository;
    private final EntityManager entityManager;

    public UserJpaAdapter(UserJpaDataRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public User save(User user) {
        Objects.requireNonNull(user, "User is required");
        if (user.id() != null && !repository.existsById(user.id())) {
            throw new IllegalArgumentException("Unknown user ID: " + user.id());
        }
        UserEntity entity = new UserEntity(user.id(), normalize(user.email()), user.password(), user.fullName(), user.role());
        UserEntity saved;
        try {
            saved = repository.save(entity);
            entityManager.flush();
        } catch (RuntimeException failure) {
            if (hasConstraint(failure, "uk_users_email")) {
                throw new IllegalStateException("User with this email already exists: " + user.email(), failure);
            }
            throw failure;
        }
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return repository.findByEmailNormalized(normalize(email)).map(UserJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(UserEntity::getId))
                .map(UserJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getPassword(), entity.getFullName(), entity.getRole());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasConstraint(Throwable failure, String expectedName) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null
                    && violation.getConstraintName().toLowerCase(Locale.ROOT)
                    .contains(expectedName.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
