package ua.kpi.absolutesvinema.repository.jpa.store;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ua.kpi.absolutesvinema.repository.jpa.entity.UserEntity;

import java.util.Optional;

public interface UserJpaDataRepository extends CrudRepository<UserEntity, Long> {
    @Query(name = "UserEntity.findByNormalizedEmail")
    Optional<UserEntity> findByEmailNormalized(@Param("email") String email);
}
