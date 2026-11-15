package ua.kpi.absolutesvinema.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.kpi.absolutesvinema.dto.ScreeningForm;
import ua.kpi.absolutesvinema.exception.ResourceNotFoundException;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.service.ScreeningService;

@Controller
@RequestMapping("/admin/screenings")
public class AdminScreeningController {
    private final ScreeningService screeningService;

    public AdminScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("screenings", screeningService.findAll());
        return "admin/screenings/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("screeningForm", new ScreeningForm());
        model.addAttribute("isNew", true);
        return "admin/screenings/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("screeningForm") ScreeningForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", true);
            return "admin/screenings/form";
        }
        Screening screening = screeningService.create(
                form.getMovieTitle(),
                form.getMovieDescription(),
                form.getMovieDurationMinutes(),
                form.getStartsAt(),
                form.getHall(),
                form.getTicketPrice()
        );
        redirect.addFlashAttribute("message", "Screening for '" + screening.movieTitle() + "' created successfully with 15 seats.");
        return "redirect:/admin/screenings";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Screening screening = screeningService.findById(id);
        model.addAttribute("screeningForm", ScreeningForm.from(screening));
        model.addAttribute("screeningId", id);
        model.addAttribute("isNew", false);
        return "admin/screenings/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("screeningForm") ScreeningForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirect) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("screeningId", id);
            model.addAttribute("isNew", false);
            return "admin/screenings/form";
        }
        screeningService.update(
                id,
                form.getMovieTitle(),
                form.getMovieDescription(),
                form.getMovieDurationMinutes(),
                form.getStartsAt(),
                form.getHall(),
                form.getTicketPrice()
        );
        redirect.addFlashAttribute("message", "Screening #" + id + " updated successfully.");
        return "redirect:/admin/screenings";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        screeningService.delete(id);
        redirect.addFlashAttribute("message", "Screening #" + id + " and all associated seats/tickets deleted successfully.");
        return "redirect:/admin/screenings";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() {
        return "screenings/not-found";
    }
}
