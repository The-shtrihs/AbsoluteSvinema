package ua.kpi.absolutesvinema.repository.jpa.store;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;

import java.util.List;

public interface ScreeningJpaDataRepository extends CrudRepository<ScreeningEntity, Long> {
    @Query("""
            select s from ScreeningEntity s
            where (
                :title is null or trim(:title) = '' or
                locate(lower(trim(:title)), lower(s.movieTitle)) > 0
            )
              and (:hall is null or trim(:hall) = ''
                   or lower(trim(s.hall)) = lower(trim(:hall)))
            order by s.id
            """)
    List<ScreeningEntity> findByTitleAndHall(@Param("title") String title, @Param("hall") String hall);
}
