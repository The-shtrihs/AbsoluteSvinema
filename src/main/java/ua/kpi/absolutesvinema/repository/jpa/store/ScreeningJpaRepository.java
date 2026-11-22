package ua.kpi.absolutesvinema.repository.jpa.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;

import java.util.List;

public interface ScreeningJpaRepository extends JpaRepository<ScreeningEntity, Long> {
    List<ScreeningEntity> findAllByOrderByIdAsc();

    @Query("""
            select s from ScreeningEntity s
            where (:title is null or locate(:title, lower(s.movieTitle)) > 0)
              and (:hall is null or lower(trim(s.hall)) = lower(:hall))
            order by s.id
            """)
    List<ScreeningEntity> search(@Param("title") String title, @Param("hall") String hall);
}
