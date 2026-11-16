package ua.kpi.absolutesvinema.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

@Tag(name = "Screenings", description = "CRUD, filtering, and pagination for cinema screenings")
@RestController
@RequestMapping("/api/screenings")
public class ScreeningRestController {

    private final ScreeningService screeningService;

    public ScreeningRestController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @Operation(
            summary = "List screenings with filtering and pagination",
            description = "Retrieves a paginated list of cinema screenings. Supports optional substring filtering " +
                    "by movie title (case-insensitive) and exact filtering by hall name. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved page of screenings",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters (page < 0, or size < 1 or > 100)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping
    public PageResponse<ScreeningResponse> list(
            @Parameter(description = "Case-insensitive substring filter for movie title", example = "Interstellar")
            @RequestParam(required = false) String title,

            @Parameter(description = "Exact filter for cinema hall name", example = "Hall 1")
            @RequestParam(required = false) String hall,

            @Parameter(description = "Zero-based page index (must be >= 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (must be between 1 and 100)", example = "10")
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

    @Operation(
            summary = "Get screening by ID",
            description = "Retrieves details of a specific cinema screening by its numeric identifier. Publicly accessible."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Screening found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScreeningResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameter type (e.g. non-numeric ID)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Screening not found for given ID",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping("/{id}")
    public ScreeningResponse getById(
            @Parameter(description = "Numeric identifier of the screening", example = "1", required = true)
            @PathVariable Long id
    ) {
        return ScreeningResponse.from(screeningService.findById(id));
    }

    @Operation(
            summary = "Create a new screening",
            description = "Creates a new cinema screening and automatically generates its 15 seats (3 rows x 5 seats). " +
                    "Requires ROLE_ADMIN, active session cookie, and valid CSRF token header.",
            security = {
                    @SecurityRequirement(name = "cookieAuth"),
                    @SecurityRequirement(name = "csrfToken")
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Screening created successfully. Location header contains URI of created resource.",
                    headers = @Header(name = "Location", description = "URI of newly created screening", schema = @Schema(type = "string")),
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScreeningResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error in request body (e.g. blank title, negative price, duration <= 0)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required (missing or invalid session cookie)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: insufficient permissions (requires ROLE_ADMIN) or invalid/missing CSRF token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
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

    @Operation(
            summary = "Fully update an existing screening",
            description = "Replaces all editable fields of an existing screening (full update / PUT semantics). " +
                    "Requires ROLE_ADMIN, active session cookie, and valid CSRF token header.",
            security = {
                    @SecurityRequirement(name = "cookieAuth"),
                    @SecurityRequirement(name = "csrfToken")
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Screening updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScreeningResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error in request body or invalid parameters",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: requires ROLE_ADMIN or invalid/missing CSRF token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Screening not found for given ID",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PutMapping("/{id}")
    public ScreeningResponse update(
            @Parameter(description = "Numeric identifier of the screening to update", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ScreeningRequest request
    ) {
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

    @Operation(
            summary = "Delete a screening",
            description = "Deletes a screening along with all its seats and associated tickets. " +
                    "Requires ROLE_ADMIN, active session cookie, and valid CSRF token header.",
            security = {
                    @SecurityRequirement(name = "cookieAuth"),
                    @SecurityRequirement(name = "csrfToken")
            }
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Screening successfully deleted"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden: requires ROLE_ADMIN or invalid/missing CSRF token",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Screening not found for given ID",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Numeric identifier of the screening to delete", example = "1", required = true)
            @PathVariable Long id
    ) {
        screeningService.delete(id);
    }
}
