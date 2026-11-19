package ua.kpi.absolutesvinema.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.absolutesvinema.exception.ResourceNotFoundException;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.repository.TicketRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScreeningService implements ScreeningManagement {
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;

    public ScreeningService(ScreeningRepository screeningRepository, SeatRepository seatRepository,
                            TicketRepository ticketRepository) {
        this.screeningRepository = screeningRepository;
        this.seatRepository = seatRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<Screening> findAll() {
        return screeningRepository.findAll();
    }

    @Override
    public List<Screening> findFiltered(String title, String hall) {
        return screeningRepository.findByTitleAndHall(title, hall);
    }

    @Override
    public Screening findById(Long id) {
        return screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening", id));
    }

    @Override
    @Transactional
    public Screening create(String movieTitle, String movieDescription, int movieDurationMinutes,
                            LocalDateTime startsAt, String hall, BigDecimal ticketPrice) {
        Screening screening = screeningRepository.save(new Screening(null, movieTitle, movieDescription,
                movieDurationMinutes, startsAt, hall, ticketPrice));
        for (int row = 1; row <= 3; row++) {
            for (int seat = 1; seat <= 5; seat++) {
                seatRepository.save(new Seat(null, screening.id(), row, seat));
            }
        }
        return screening;
    }

    @Override
    public Screening update(Long id, String movieTitle, String movieDescription, int movieDurationMinutes,
                            LocalDateTime startsAt, String hall, BigDecimal ticketPrice) {
        Screening existing = findById(id);
        return screeningRepository.save(new Screening(existing.id(), movieTitle, movieDescription,
                movieDurationMinutes, startsAt, hall, ticketPrice));
    }

    public List<Seat> findSeats(Long screeningId) {
        findById(screeningId);
        return seatRepository.findByScreeningId(screeningId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        List<Seat> seats = findSeats(id);
        List<Long> seatIds = seats.stream().map(Seat::id).toList();
        ticketRepository.findBySeatIdIn(seatIds)
                .forEach(ticket -> ticketRepository.deleteById(ticket.id()));
        seats.forEach(seat -> seatRepository.deleteById(seat.id()));
        screeningRepository.deleteById(id);
    }
}
