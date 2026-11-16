package ua.kpi.absolutesvinema.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ua.kpi.absolutesvinema.model.Screening;
import ua.kpi.absolutesvinema.service.ScreeningService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ScreeningPatchTest {

    static final String JSON_PATCH  = "application/json-patch+json";
    static final String MERGE_PATCH = "application/merge-patch+json";
    static final String PROBLEM     = "application/problem+json";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ScreeningService screeningService;

    private MockMvc mockMvc;

    private Screening screening;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        screening = screeningService.create(
                "Patch Target " + System.currentTimeMillis(),
                "Original description",
                120,
                LocalDateTime.of(2028, 1, 10, 18, 0),
                "Hall P",
                new BigDecimal("200.00")
        );
    }

    // ── JSON Patch (RFC 6902) ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_replace_movieTitle_returns200() throws Exception {
        String patch = """
                [{"op":"replace","path":"/movieTitle","value":"Replaced Title"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(screening.id().intValue())))
                .andExpect(jsonPath("$.movieTitle", is("Replaced Title")))
                .andExpect(jsonPath("$.movieDescription", is("Original description")));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_multipleOperations_returns200() throws Exception {
        String patch = """
                [
                  {"op":"replace","path":"/movieTitle","value":"Multi Patched Title"},
                  {"op":"replace","path":"/ticketPrice","value":150.00}
                ]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieTitle", is("Multi Patched Title")))
                .andExpect(jsonPath("$.ticketPrice").value(150.00));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_copy_operation_returns200() throws Exception {
        // Copy movieTitle value to movieDescription
        String patch = """
                [{"op":"copy","from":"/movieTitle","path":"/movieDescription"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieDescription", is(screening.movieTitle())));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_test_condition_success_then_replace() throws Exception {
        String patch = String.format("""
                [
                  {"op":"test","path":"/movieTitle","value":"%s"},
                  {"op":"replace","path":"/movieDurationMinutes","value":77}
                ]
                """, screening.movieTitle());

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieDurationMinutes", is(77)));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_test_condition_failure_returns409() throws Exception {
        String patch = """
                [{"op":"test","path":"/movieTitle","value":"WRONG_VALUE_THAT_NEVER_MATCHES"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Patch Test Condition Failed"));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_remove_requiredField_returns400() throws Exception {
        String patch = """
                [{"op":"remove","path":"/movieTitle"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_move_requiredField_returns400() throws Exception {
        // Move removes from source -> movieTitle becomes missing/null
        String patch = """
                [{"op":"move","from":"/movieTitle","path":"/movieDescription"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_forbiddenIdPatch_returns400() throws Exception {
        String patch = """
                [{"op":"replace","path":"/id","value":999}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_unknownField_returns400() throws Exception {
        String patch = """
                [{"op":"replace","path":"/unknownField","value":"hacked"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_invalidType_returns400() throws Exception {
        String patch = """
                [{"op":"replace","path":"/movieDurationMinutes","value":"not-a-number"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_malformedBody_returns400() throws Exception {
        String patch = "this is not valid JSON patch";

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_patchingToBlankTitle_returns400() throws Exception {
        String patch = """
                [{"op":"replace","path":"/movieTitle","value":"   "}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_patchingToNegativeDuration_returns400() throws Exception {
        String patch = """
                [{"op":"replace","path":"/movieDurationMinutes","value":-1}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_nonExistentScreening_returns404() throws Exception {
        String patch = """
                [{"op":"replace","path":"/movieTitle","value":"Ghost"}]
                """;

        mockMvc.perform(patch("/api/screenings/999999")
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── JSON Merge Patch (RFC 7396) ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void mergePatch_partialUpdate_returns200() throws Exception {
        String patch = """
                {"movieTitle":"Merged Title","movieDurationMinutes":88}
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieTitle", is("Merged Title")))
                .andExpect(jsonPath("$.movieDurationMinutes", is(88)))
                .andExpect(jsonPath("$.movieDescription", is("Original description")))
                .andExpect(jsonPath("$.hall", is("Hall P")));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void mergePatch_nullRemoval_failsValidation_returns400() throws Exception {
        // RFC 7396: null removes property -> movieTitle becomes null -> fails @NotBlank
        String patch = """
                {"movieTitle": null}
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void mergePatch_forbiddenIdPatch_returns400() throws Exception {
        String patch = """
                {"id": 999}
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void mergePatch_unknownField_returns400() throws Exception {
        String patch = """
                {"extraProp": "illegal"}
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void mergePatch_invalidType_returns400() throws Exception {
        String patch = """
                {"movieDurationMinutes": "invalid_number"}
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void mergePatch_patchingToBlankTitle_returns400() throws Exception {
        String patch = """
                {"movieTitle":""}
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void mergePatch_malformedBody_returns400() throws Exception {
        String patch = "not-json-at-all";

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void mergePatch_nonExistentScreening_returns404() throws Exception {
        String patch = """
                {"movieTitle":"Ghost Merge"}
                """;

        mockMvc.perform(patch("/api/screenings/999999")
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Atomicity / Unchanged State on Failure ─────────────────────────────────

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void failedPatch_leavesScreeningStateUnchanged() throws Exception {
        String originalTitle = screening.movieTitle();
        Integer originalDuration = screening.movieDurationMinutes();

        // Patch contains invalid duration after valid title change -> must reject and not apply
        String patch = String.format("""
                [
                  {"op":"replace","path":"/movieTitle","value":"New Title"},
                  {"op":"replace","path":"/movieDurationMinutes","value":-50}
                ]
                """);

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isBadRequest());

        Screening reloaded = screeningService.findById(screening.id());
        assertThat(reloaded.movieTitle()).isEqualTo(originalTitle);
        assertThat(reloaded.movieDurationMinutes()).isEqualTo(originalDuration);
    }

    // ── Authorization & CSRF ──────────────────────────────────────────────────

    @Test
    void jsonPatch_anonymous_withCsrf_returns401() throws Exception {
        String patch = """
                [{"op":"replace","path":"/movieTitle","value":"Hacked"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void jsonPatch_admin_withoutCsrf_returns403() throws Exception {
        String patch = """
                [{"op":"replace","path":"/movieTitle","value":"Hacked No CSRF"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@svinema.ua", roles = "USER")
    void jsonPatch_regularUser_returns403() throws Exception {
        String patch = """
                [{"op":"replace","path":"/movieTitle","value":"Hacked by user"}]
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(JSON_PATCH)
                        .content(patch))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    @WithMockUser(username = "user@svinema.ua", roles = "USER")
    void mergePatch_regularUser_returns403() throws Exception {
        String patch = """
                {"movieTitle":"Hacked by user"}
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType(MERGE_PATCH)
                        .content(patch))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    // ── Unsupported Content-Type ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@svinema.ua", roles = "ADMIN")
    void patch_wrongContentType_returns415() throws Exception {
        String patch = """
                {"movieTitle":"Wrong Content Type"}
                """;

        mockMvc.perform(patch("/api/screenings/" + screening.id())
                        .with(csrf())
                        .contentType("application/json")
                        .content(patch))
                .andExpect(status().isUnsupportedMediaType());
    }
}
