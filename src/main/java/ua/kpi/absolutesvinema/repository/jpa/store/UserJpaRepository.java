package ua.kpi.absolutesvinema.repository.jpa.store;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.kpi.absolutesvinema.repository.jpa.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    List<UserEntity> findAllByOrderByIdAsc();

    Optional<UserEntity> findByEmail(String email);
}
