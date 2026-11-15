package ua.kpi.absolutesvinema.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.kpi.absolutesvinema.dto.TicketUpdateForm;
import ua.kpi.absolutesvinema.exception.ResourceNotFoundException;
import ua.kpi.absolutesvinema.model.Seat;
import ua.kpi.absolutesvinema.model.Ticket;
import ua.kpi.absolutesvinema.service.ScreeningService;
import ua.kpi.absolutesvinema.service.TicketService;

@Controller
@RequestMapping("/admin/tickets")
public class AdminTicketController {
    private final TicketService ticketService;
    private final ScreeningService screeningService;

    public AdminTicketController(TicketService ticketService, ScreeningService screeningService) {
        this.ticketService = ticketService;
        this.screeningService = screeningService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tickets", ticketService.findAllWithDetails());
        return "admin/tickets/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Ticket ticket = ticketService.findById(id);
        Seat seat = ticketService.findSeat(ticket.seatId());
        var screening = screeningService.findById(seat.screeningId());

        model.addAttribute("ticket", ticket);
        model.addAttribute("seat", seat);
        model.addAttribute("screening", screening);
        model.addAttribute("ticketForm", TicketUpdateForm.from(ticket));
        return "admin/tickets/form";
    }

    @PostMapping("/{id}/edit")
    public String updateCustomerName(@PathVariable Long id,
                                     @Valid @ModelAttribute("ticketForm") TicketUpdateForm form,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            Ticket ticket = ticketService.findById(id);
            Seat seat = ticketService.findSeat(ticket.seatId());
            var screening = screeningService.findById(seat.screeningId());
            model.addAttribute("ticket", ticket);
            model.addAttribute("seat", seat);
            model.addAttribute("screening", screening);
            return "admin/tickets/form";
        }
        ticketService.updateCustomerName(id, form.getCustomerName());
        redirect.addFlashAttribute("message", "Ticket #" + id + " customer name updated successfully.");
        return "redirect:/admin/tickets";
    }

    @PostMapping("/{id}/cancel")
    public String cancelTicket(@PathVariable Long id, RedirectAttributes redirect) {
        ticketService.cancel(id);
        redirect.addFlashAttribute("message", "Ticket #" + id + " cancelled successfully. Seat is now available.");
        return "redirect:/admin/tickets";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() {
        return "screenings/not-found";
    }
}
