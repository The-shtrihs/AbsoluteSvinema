package ua.kpi.absolutesvinema.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.kpi.absolutesvinema.exception.ResourceNotFoundException;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.service.ScreeningService;
import ua.kpi.absolutesvinema.service.TicketService;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/screenings")
public class ScreeningController {
    private final ScreeningService screeningService;
    private final TicketService ticketService;

    public ScreeningController(ScreeningService screeningService, TicketService ticketService) {
        this.screeningService = screeningService;
        this.ticketService = ticketService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("screenings", screeningService.findAll());
        return "screenings/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("screening", screeningService.findById(id));
        var seats = screeningService.findSeats(id);
        model.addAttribute("seats", seats);
        model.addAttribute("bookedSeatIds", seats.stream()
                .map(Seat::id).filter(ticketService::isSeatBooked).collect(Collectors.toSet()));
        return "screenings/details";
    }

    @PostMapping("/{id}/book")
    public String book(@PathVariable Long id, @RequestParam Long seatId,
                       @RequestParam String customerName, RedirectAttributes redirect) {
        var seats = screeningService.findSeats(id);
        if (seats.stream().noneMatch(seat -> seat.id().equals(seatId))) {
            throw new ResourceNotFoundException("Seat in screening " + id, seatId);
        }
        try {
            ticketService.book(seatId, customerName);
            redirect.addFlashAttribute("message", "Seat booked successfully.");
        } catch (IllegalStateException occupied) {
            redirect.addFlashAttribute("error", "This seat is already booked. Please choose another.");
        } catch (IllegalArgumentException invalid) {
            redirect.addFlashAttribute("error", "Please enter a non-blank customer name.");
        }
        return "redirect:/screenings/" + id;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() {
        return "screenings/not-found";
    }
}
