package ua.kpi.absolutesvinema.repository.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ua.kpi.absolutesvinema.repository.jpa.entity.UserEntity;

import java.util.Optional;

public interface UserJpaDataRepository extends CrudRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByNormalizedEmail(@Param("email") String email);
}
