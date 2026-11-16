package ua.kpi.absolutesvinema.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.opentmf.commons.patch.JsonMergePatch;
import org.opentmf.commons.patch.JsonPatch;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ua.kpi.absolutesvinema.dto.api.ScreeningRequest;
import ua.kpi.absolutesvinema.dto.api.ScreeningResponse;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.util.Iterator;
import java.util.Set;

@Tag(name = "Screening Partial Updates", description = "RFC 6902 JSON Patch and RFC 7396 JSON Merge Patch operations")
@RestController
@RequestMapping("/api/screenings")
public class ScreeningPatchController {

    public static final String JSON_PATCH_MEDIA_TYPE  = "application/json-patch+json";
    public static final String MERGE_PATCH_MEDIA_TYPE = "application/merge-patch+json";

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "movieTitle", "movieDescription", "movieDurationMinutes",
            "startsAt", "hall", "ticketPrice"
    );

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/movieTitle", "/movieDescription", "/movieDurationMinutes",
            "/startsAt", "/hall", "/ticketPrice"
    );

    private final ScreeningService screeningService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ScreeningPatchController(ScreeningService screeningService,
                                    ObjectMapper objectMapper,
                                    Validator validator) {
        this.screeningService = screeningService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }


    @Operation(
            summary = "Partially update screening using JSON Patch (RFC 6902) or JSON Merge Patch (RFC 7396)",
            description = "Applies an array of standard RFC 6902 patch operations (add, remove, replace, move, copy, test) " +
                    "to an existing screening resource. Only editable fields (movieTitle, movieDescription, movieDurationMinutes, " +
                    "startsAt, hall, ticketPrice) may be targeted. Attempts to modify id, seats, tickets, or unknown fields are rejected. " +
                    "Atomic persistence: changes are saved only if all operations succeed and validation passes. " +
                    "Requires ROLE_ADMIN, active session cookie, and valid CSRF token header.",
            security = {
                    @SecurityRequirement(name = "cookieAuth"),
                    @SecurityRequirement(name = "csrfToken")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Partial update using RFC 6902 JSON Patch " +
                            "or RFC 7396 JSON Merge Patch. " +
                            "Select the appropriate Content-Type.",
                    required = true,
                    content = @Content(
                            mediaType = JSON_PATCH_MEDIA_TYPE,
                            examples = {
                                    @ExampleObject(
                                            name = "Replace title and price",
                                            summary = "Replace movieTitle and ticketPrice",
                                            value = """
                                                    [
                                                      {"op": "replace", "path": "/movieTitle", "value": "Interstellar (IMAX)"},
                                                      {"op": "replace", "path": "/ticketPrice", "value": 280.00}
                                                    ]
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Test and replace duration",
                                            summary = "Verify title with test op then replace duration",
                                            value = """
                                                    [
                                                      {"op": "test", "path": "/movieTitle", "value": "Interstellar"},
                                                      {"op": "replace", "path": "/movieDurationMinutes", "value": 175}
                                                    ]
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Copy operation",
                                            summary = "Copy title into description",
                                            value = """
                                                    [
                                                      {"op": "copy", "from": "/movieTitle", "path": "/movieDescription"}
                                                    ]
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Screening successfully updated via JSON Patch",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScreeningResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed patch body, forbidden/unknown patch path, or Bean Validation failure on patched result",
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
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "JSON Patch 'test' condition failed (precondition mismatch)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported Media Type (expected Content-Type: application/json-patch+json)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PatchMapping(value = "/{id}", consumes = JSON_PATCH_MEDIA_TYPE)
    public ResponseEntity<ScreeningResponse> patchJsonPatch(
            @Parameter(description = "Numeric identifier of screening to patch", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(array = @ArraySchema(schema = @Schema(type = "object", example = """
                    {"op": "replace", "path": "/movieTitle", "value": "Interstellar (IMAX)"}
                    """)))
            @RequestBody String patchBody) {

        Screening existing = screeningService.findById(id);
        JsonNode patchNode = parseJson(patchBody);

        if (!patchNode.isArray()) {
            throw new IllegalArgumentException("JSON Patch payload must be a JSON array (RFC 6902)");
        }

        for (JsonNode op : patchNode) {
            validateOperationPaths(op);
        }

        JsonPatch patch = JsonPatch.fromJsonNode(patchNode);
        JsonNode original = editableTree(existing);
        JsonNode patched = patch.apply(original);

        ScreeningRequest request = deserializeAndValidate(patched);
        Screening updated = applyUpdate(id, request);
        return ResponseEntity.ok(ScreeningResponse.from(updated));
    }


    @Operation(
            summary = "Partially update screening using JSON Patch (RFC 6902) or JSON Merge Patch (RFC 7396)",
            description = "Applies an RFC 7396 merge patch document to an existing screening resource. " +
                    "Properties present in the patch object overwrite existing values; explicit null indicates property removal " +
                    "(which fails validation for required screening fields). Only editable fields may be provided; " +
                    "unknown or forbidden fields (id, seats, tickets) are rejected. Atomic persistence. " +
                    "Requires ROLE_ADMIN, active session cookie, and valid CSRF token header.",
            security = {
                    @SecurityRequirement(name = "cookieAuth"),
                    @SecurityRequirement(name = "csrfToken")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "RFC 7396 JSON Merge Patch document (JSON object)",
                    required = true,
                    content = @Content(
                            mediaType = MERGE_PATCH_MEDIA_TYPE,
                            examples = {
                                    @ExampleObject(
                                            name = "Update title and duration",
                                            summary = "Partial update of title and duration only",
                                            value = """
                                                    {
                                                      "movieTitle": "Interstellar (Extended Cut)",
                                                      "movieDurationMinutes": 180
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Update price and hall",
                                            summary = "Partial update of ticket price and hall",
                                            value = """
                                                    {
                                                      "hall": "Hall 2 (VIP)",
                                                      "ticketPrice": 320.00
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Screening successfully updated via Merge Patch",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScreeningResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed merge patch, unknown/forbidden fields, or validation failure (e.g. required field removed via null)",
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
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Unsupported Media Type (expected Content-Type: application/merge-patch+json)",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PatchMapping(value = "/{id}", consumes = MERGE_PATCH_MEDIA_TYPE)
    public ResponseEntity<ScreeningResponse> patchMergePatch(
            @Parameter(description = "Numeric identifier of screening to merge-patch", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(schema = @Schema(type = "object"))
            @RequestBody String patchBody) {

        Screening existing = screeningService.findById(id);
        JsonNode patchNode = parseJson(patchBody);

        if (!patchNode.isObject()) {
            throw new IllegalArgumentException("Merge Patch payload must be a JSON object (RFC 7396)");
        }

        for (Iterator<String> it = patchNode.propertyNames().iterator(); it.hasNext(); ) {
            String field = it.next();
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Forbidden or unknown merge patch field: " + field);
            }
        }

        JsonMergePatch mergePatch = JsonMergePatch.of(patchNode);
        JsonNode original = editableTree(existing);
        JsonNode patched = mergePatch.apply(original);

        ScreeningRequest request = deserializeAndValidate(patched);
        Screening updated = applyUpdate(id, request);
        return ResponseEntity.ok(ScreeningResponse.from(updated));
    }


    private JsonNode editableTree(Screening screening) {
        ScreeningRequest editable = new ScreeningRequest(
                screening.movieTitle(),
                screening.movieDescription(),
                screening.movieDurationMinutes(),
                screening.startsAt(),
                screening.hall(),
                screening.ticketPrice()
        );
        return objectMapper.valueToTree(editable);
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Request body must not be blank");
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed JSON: " + e.getMessage(), e);
        }
    }

    private void validateOperationPaths(JsonNode op) {
        if (!op.isObject()) {
            throw new IllegalArgumentException("Each JSON Patch operation must be an object");
        }
        JsonNode pathNode = op.get("path");
        if (pathNode == null || !pathNode.isString()) {
            throw new IllegalArgumentException("Operation is missing a valid 'path' property");
        }
        if (!ALLOWED_PATHS.contains(pathNode.stringValue())) {
            throw new IllegalArgumentException(
                    "Forbidden or unknown patch path: " + pathNode.stringValue());
        }
        if (op.has("from")) {
            JsonNode fromNode = op.get("from");
            if (fromNode == null || !fromNode.isString()
                    || !ALLOWED_PATHS.contains(fromNode.stringValue())) {
                throw new IllegalArgumentException(
                        "Forbidden or unknown 'from' path: "
                                + (fromNode != null ? fromNode.stringValue() : null));
            }
        }
    }

    private ScreeningRequest deserializeAndValidate(JsonNode patched) {
        if (!patched.isObject()) {
            throw new IllegalArgumentException("Patched result must be a JSON object");
        }
        for (Iterator<String> it = patched.propertyNames().iterator(); it.hasNext(); ) {
            String field = it.next();
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new IllegalArgumentException(
                        "Forbidden or unknown field in patched object: " + field);
            }
        }

        ScreeningRequest request;
        try {
            request = objectMapper.treeToValue(patched, ScreeningRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid field data type in patched result: " + e.getMessage(), e);
        }

        Set<ConstraintViolation<ScreeningRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return request;
    }

    private Screening applyUpdate(Long id, ScreeningRequest req) {
        return screeningService.update(
                id,
                req.movieTitle(),
                req.movieDescription(),
                req.movieDurationMinutes(),
                req.startsAt(),
                req.hall(),
                req.ticketPrice()
        );
    }
}
