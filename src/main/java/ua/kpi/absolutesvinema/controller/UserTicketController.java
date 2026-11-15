package ua.kpi.absolutesvinema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.kpi.absolutesvinema.model.User;
import ua.kpi.absolutesvinema.service.TicketService;
import ua.kpi.absolutesvinema.service.UserService;

import java.security.Principal;

@Controller
@RequestMapping("/tickets")
public class UserTicketController {
    private final TicketService ticketService;
    private final UserService userService;

    public UserTicketController(TicketService ticketService, UserService userService) {
        this.ticketService = ticketService;
        this.userService = userService;
    }

    @GetMapping("/my")
    public String myTickets(Principal principal, Model model) {
        User user = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));

        var userTickets = ticketService.findUserTicketsWithDetails(user.id());
        model.addAttribute("tickets", userTickets);
        model.addAttribute("currentUser", user);
        return "tickets/my";
    }

    @PostMapping("/{id}/cancel")
    public String cancelTicket(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        User user = userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("User not found: " + principal.getName()));

        try {
            ticketService.cancelUserTicket(id, user.id());
            redirect.addFlashAttribute("message", "Ticket cancelled successfully. The seat is now available.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tickets/my";
    }
}
