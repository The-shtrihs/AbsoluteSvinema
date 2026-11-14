package ua.kpi.absolutesvinema.repository;

import ua.kpi.absolutesvinema.model.Screening;

import java.util.List;
import java.util.Optional;

public interface ScreeningRepository {
    /** Creates when ID is null; otherwise updates an existing entity or throws IllegalArgumentException. */
    Screening save(Screening screening);

    Optional<Screening> findById(Long id);

    List<Screening> findAll();

    /** Deleting a missing ID has no effect. */
    void deleteById(Long id);
}
