package ua.kpi.absolutesvinema.repository.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ua.kpi.absolutesvinema.repository.jpa.entity.ScreeningEntity;

import java.util.List;
import java.util.Locale;

public interface ScreeningJpaDataRepository extends CrudRepository<ScreeningEntity, Long> {

    @Query("""
            SELECT s FROM ScreeningEntity s
            WHERE (:titlePattern IS NULL OR LOWER(s.movieTitle) LIKE :titlePattern ESCAPE '\\')
              AND (:hall IS NULL OR LOWER(TRIM(s.hall)) = :hall)
            ORDER BY s.id
            """)
    List<ScreeningEntity> searchByTitlePatternAndHall(
            @Param("titlePattern") String titlePattern,
            @Param("hall") String hall);

    default List<ScreeningEntity> findByTitleAndHall(String title, String hall) {
        String titlePattern = title == null || title.isBlank()
                ? null
                : "%" + escapeLike(title.trim().toLowerCase(Locale.ROOT)) + "%";
        String normalizedHall = hall == null || hall.isBlank()
                ? null
                : hall.trim().toLowerCase(Locale.ROOT);
        return searchByTitlePatternAndHall(titlePattern, normalizedHall);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
