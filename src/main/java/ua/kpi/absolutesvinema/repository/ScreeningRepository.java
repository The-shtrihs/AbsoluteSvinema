package ua.kpi.absolutesvinema.repository;

import ua.kpi.absolutesvinema.model.Screening;

import java.util.List;
import java.util.Optional;

public interface ScreeningRepository {

    Screening save(Screening screening);

    Optional<Screening> findById(Long id);

    List<Screening> findAll();

    void deleteById(Long id);
}
