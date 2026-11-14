package ua.kpi.absolutesvinema.service;

import org.springframework.stereotype.Service;
import ua.kpi.absolutesvinema.exception.ResourceNotFoundException;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ScreeningRepository screeningRepository;

    public TicketService(TicketRepository ticketRepository, SeatRepository seatRepository,
                         ScreeningRepository screeningRepository) {
        this.ticketRepository = ticketRepository;
        this.seatRepository = seatRepository;
        this.screeningRepository = screeningRepository;
    }

    public Ticket book(Long seatId, String customerName) {
        Seat seat = findSeat(seatId);
        screeningRepository.findById(seat.screeningId())
                .orElseThrow(() -> new ResourceNotFoundException("Screening", seat.screeningId()));
        Ticket ticket = new Ticket(null, seatId, customerName, LocalDateTime.now());
        if (ticketRepository.findBySeatId(seatId).isPresent()) {
            throw new IllegalStateException("Seat already has a ticket: " + seatId);
        }
        // The repository also checks availability atomically with saving.
        return ticketRepository.save(ticket);
    }

    public Ticket findById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    public Ticket updateCustomerName(Long id, String customerName) {
        Ticket existing = findById(id);
        return ticketRepository.save(new Ticket(existing.id(), existing.seatId(),
                customerName, existing.bookedAt()));
    }

    public void cancel(Long id) {
        findById(id);
        ticketRepository.deleteById(id);
    }

    public boolean isSeatBooked(Long seatId) {
        findSeat(seatId);
        return ticketRepository.findBySeatId(seatId).isPresent();
    }

    private Seat findSeat(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", id));
    }
}
