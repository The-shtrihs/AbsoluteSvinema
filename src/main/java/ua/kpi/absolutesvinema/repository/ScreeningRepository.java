package ua.kpi.absolutesvinema.repository;

import ua.kpi.absolutesvinema.model.Screening;

import java.util.List;
import java.util.Optional;

public interface ScreeningRepository {

    Screening save(Screening screening);

    Optional<Screening> findById(Long id);

    List<Screening> findAll();

    /**
     * Case-insensitive title substring and hall equality; a null/blank criterion is ignored.
     * Results are ordered by ID.
     */
    List<Screening> findByTitleAndHall(String title, String hall);

    void deleteById(Long id);
}
