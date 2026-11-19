package ua.kpi.absolutesvinema.service;

import ua.kpi.absolutesvinema.model.Screening;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business operations on screenings used by the REST API.
 * {@link #create} and {@link #delete} write several tables and run in a single transaction.
 */
public interface ScreeningManagement {

    List<Screening> findFiltered(String title, String hall);

    Screening findById(Long id);

    /** Inserts the screening and its 15 seats atomically. */
    Screening create(String movieTitle, String movieDescription, int movieDurationMinutes,
                     LocalDateTime startsAt, String hall, BigDecimal ticketPrice);

    Screening update(Long id, String movieTitle, String movieDescription, int movieDurationMinutes,
                     LocalDateTime startsAt, String hall, BigDecimal ticketPrice);

    /** Deletes tickets, then seats, then the screening atomically. */
    void delete(Long id);
}
