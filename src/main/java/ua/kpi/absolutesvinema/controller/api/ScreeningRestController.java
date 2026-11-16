package ua.kpi.absolutesvinema.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ua.kpi.absolutesvinema.dto.api.PageResponse;
import ua.kpi.absolutesvinema.dto.api.ScreeningRequest;
import ua.kpi.absolutesvinema.dto.api.ScreeningResponse;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/screenings")
public class ScreeningRestController {

    private final ScreeningService screeningService;

    public ScreeningRestController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @GetMapping
    public PageResponse<ScreeningResponse> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String hall,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        List<Screening> filtered = screeningService.findFiltered(title, hall);
        long totalElements = filtered.size();
        int totalPages = size == 0 || totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        List<ScreeningResponse> content = filtered.stream()
                .skip((long) page * size)
                .limit(size)
                .map(ScreeningResponse::from)
                .toList();

        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    @GetMapping("/{id}")
    public ScreeningResponse getById(@PathVariable Long id) {
        return ScreeningResponse.from(screeningService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ScreeningResponse> create(@Valid @RequestBody ScreeningRequest request) {
        Screening created = screeningService.create(
                request.movieTitle(),
                request.movieDescription(),
                request.movieDurationMinutes(),
                request.startsAt(),
                request.hall(),
                request.ticketPrice()
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(ScreeningResponse.from(created));
    }

    @PutMapping("/{id}")
    public ScreeningResponse update(@PathVariable Long id, @Valid @RequestBody ScreeningRequest request) {
        Screening updated = screeningService.update(
                id,
                request.movieTitle(),
                request.movieDescription(),
                request.movieDurationMinutes(),
                request.startsAt(),
                request.hall(),
                request.ticketPrice()
        );
        return ScreeningResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        screeningService.delete(id);
    }
}
