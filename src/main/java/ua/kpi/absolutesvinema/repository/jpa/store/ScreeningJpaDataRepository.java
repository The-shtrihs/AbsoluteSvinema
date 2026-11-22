package ua.kpi.absolutesvinema.repository.jpa.store;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;

import java.util.List;

public interface ScreeningJpaDataRepository extends CrudRepository<ScreeningEntity, Long> {
    @Query("""
            select s from ScreeningEntity s
            where (:title = '' or locate(lower(:title), lower(s.movieTitle)) > 0)
              and (:hall = '' or lower(trim(s.hall)) = lower(:hall))
            order by s.id
            """)
    List<ScreeningEntity> findByTitleAndHall(@Param("title") String title, @Param("hall") String hall);
}
