package ua.kpi.absolutesvinema.service;

import ua.kpi.absolutesvinema.model.Screening;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ScreeningManagement {

    List<Screening> findFiltered(String title, String hall);

    Screening findById(Long id);

    Screening create(String movieTitle, String movieDescription, int movieDurationMinutes,
                     LocalDateTime startsAt, String hall, BigDecimal ticketPrice);

    Screening update(Long id, String movieTitle, String movieDescription, int movieDurationMinutes,
                     LocalDateTime startsAt, String hall, BigDecimal ticketPrice);

    void delete(Long id);
}
