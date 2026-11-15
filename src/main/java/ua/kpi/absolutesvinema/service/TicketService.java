package ua.kpi.absolutesvinema.service;

import org.springframework.stereotype.Service;
import ua.kpi.absolutesvinema.exception.ResourceNotFoundException;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.model.TicketDetails;
import ua.kpi.absolutesvinema.repository.ScreeningRepository;
import ua.kpi.absolutesvinema.repository.SeatRepository;
import ua.kpi.absolutesvinema.repository.TicketRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {
    private final Clock clock;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ScreeningRepository screeningRepository;

    public TicketService(TicketRepository ticketRepository, SeatRepository seatRepository,
                         ScreeningRepository screeningRepository, Clock clock) {
        this.clock = clock;
        this.ticketRepository = ticketRepository;
        this.seatRepository = seatRepository;
        this.screeningRepository = screeningRepository;
    }

    public Ticket book(Long seatId, String customerName) {
        return book(seatId, null, customerName);
    }

    public Ticket book(Long seatId, Long userId, String customerName) {
        Seat seat = findSeat(seatId);
        screeningRepository.findById(seat.screeningId())
                .orElseThrow(() -> new ResourceNotFoundException("Screening", seat.screeningId()));
        Ticket ticket = new Ticket(null, seatId, userId, customerName, LocalDateTime.now(clock));
        if (ticketRepository.findBySeatId(seatId).isPresent()) {
            throw new IllegalStateException("Seat already has a ticket: " + seatId);
        }

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
        return ticketRepository.save(new Ticket(existing.id(), existing.seatId(), existing.userId(),
                customerName, existing.bookedAt()));
    }

    public void cancel(Long id) {
        findById(id);
        ticketRepository.deleteById(id);
    }

    public void cancelUserTicket(Long ticketId, Long userId) {
        Ticket ticket = findById(ticketId);
        if (ticket.userId() == null || !ticket.userId().equals(userId)) {
            throw new IllegalStateException("You cannot cancel another user's ticket.");
        }
        cancel(ticketId);
    }

    public boolean isSeatBooked(Long seatId) {
        findSeat(seatId);
        return ticketRepository.findBySeatId(seatId).isPresent();
    }

    public List<TicketDetails> findAllWithDetails() {
        return ticketRepository.findAll().stream()
                .map(t -> {
                    Seat seat = findSeat(t.seatId());
                    Screening screening = screeningRepository.findById(seat.screeningId())
                            .orElseThrow(() -> new ResourceNotFoundException("Screening", seat.screeningId()));
                    return new TicketDetails(t, seat, screening);
                })
                .toList();
    }

    public List<TicketDetails> findUserTicketsWithDetails(Long userId) {
        return ticketRepository.findByUserId(userId).stream()
                .map(t -> {
                    Seat seat = findSeat(t.seatId());
                    Screening screening = screeningRepository.findById(seat.screeningId())
                            .orElseThrow(() -> new ResourceNotFoundException("Screening", seat.screeningId()));
                    return new TicketDetails(t, seat, screening);
                })
                .toList();
    }

    public Seat findSeat(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", id));
    }
}
