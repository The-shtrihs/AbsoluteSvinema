package ua.kpi.absolutesvinema.controller.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.opentmf.commons.patch.JsonMergePatch;
import org.opentmf.commons.patch.JsonPatch;
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

/**
 * Handles PATCH /api/screenings/{id} for two patch formats:
 * <ul>
 *   <li>RFC 6902 JSON Patch  — Content-Type: application/json-patch+json</li>
 *   <li>RFC 7396 JSON Merge Patch — Content-Type: application/merge-patch+json</li>
 * </ul>
 * Both require ROLE_ADMIN (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/screenings")
public class ScreeningPatchController {

    public static final String JSON_PATCH_MEDIA_TYPE  = "application/json-patch+json";
    public static final String MERGE_PATCH_MEDIA_TYPE = "application/merge-patch+json";

    /** Fields that may be patched (matching ScreeningRequest record components). */
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "movieTitle", "movieDescription", "movieDurationMinutes",
            "startsAt", "hall", "ticketPrice"
    );

    /** Valid JSON Pointer paths (top-level only). */
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

    // ── RFC 6902 JSON Patch ─────────────────────────────────────────────────

    @PatchMapping(value = "/{id}", consumes = JSON_PATCH_MEDIA_TYPE)
    public ResponseEntity<ScreeningResponse> patchJsonPatch(
            @PathVariable Long id,
            @RequestBody String patchBody) {

        Screening existing = screeningService.findById(id);           // 404 if absent
        JsonNode patchNode = parseJson(patchBody);

        if (!patchNode.isArray()) {
            throw new IllegalArgumentException("JSON Patch payload must be a JSON array (RFC 6902)");
        }

        // Validate every operation targets only allowed fields
        for (JsonNode op : patchNode) {
            validateOperationPaths(op);
        }

        JsonPatch patch = JsonPatch.fromJsonNode(patchNode);          // JsonPatchException on bad ops
        JsonNode original = editableTree(existing);
        JsonNode patched = patch.apply(original);                     // JsonPatchException on test fail etc.

        ScreeningRequest request = deserializeAndValidate(patched);
        Screening updated = applyUpdate(id, request);
        return ResponseEntity.ok(ScreeningResponse.from(updated));
    }

    // ── RFC 7396 JSON Merge Patch ───────────────────────────────────────────

    @PatchMapping(value = "/{id}", consumes = MERGE_PATCH_MEDIA_TYPE)
    public ResponseEntity<ScreeningResponse> patchMergePatch(
            @PathVariable Long id,
            @RequestBody String patchBody) {

        Screening existing = screeningService.findById(id);           // 404 if absent
        JsonNode patchNode = parseJson(patchBody);

        if (!patchNode.isObject()) {
            throw new IllegalArgumentException("Merge Patch payload must be a JSON object (RFC 7396)");
        }

        // Reject forbidden / unknown top-level fields
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

    // ── private helpers ─────────────────────────────────────────────────────

    /**
     * Builds a detached JSON tree containing only the editable fields of a
     * screening, matching the shape of {@link ScreeningRequest}.
     */
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

    /**
     * Parses raw JSON text; throws {@link IllegalArgumentException} on malformed input.
     */
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

    /**
     * Validates that a single RFC 6902 operation targets only allowed paths.
     */
    private void validateOperationPaths(JsonNode op) {
        if (!op.isObject()) {
            throw new IllegalArgumentException("Each JSON Patch operation must be an object");
        }
        // "path" is required by RFC 6902
        JsonNode pathNode = op.get("path");
        if (pathNode == null || !pathNode.isString()) {
            throw new IllegalArgumentException("Operation is missing a valid 'path' property");
        }
        if (!ALLOWED_PATHS.contains(pathNode.stringValue())) {
            throw new IllegalArgumentException(
                    "Forbidden or unknown patch path: " + pathNode.stringValue());
        }
        // "from" is used by move / copy
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

    /**
     * Deserialises the patched JSON tree into a {@link ScreeningRequest} and
     * runs Bean Validation constraints explicitly.
     */
    private ScreeningRequest deserializeAndValidate(JsonNode patched) {
        if (!patched.isObject()) {
            throw new IllegalArgumentException("Patched result must be a JSON object");
        }
        // Safety: reject any extra fields that the library may have introduced
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
