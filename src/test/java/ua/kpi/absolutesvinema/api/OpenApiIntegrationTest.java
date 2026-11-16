package ua.kpi.absolutesvinema.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class OpenApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void apiDocs_returns200AndValidOpenApiJson() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title", is("AbsoluteSvinema REST API")))
                .andExpect(jsonPath("$.paths['/api/screenings']").exists())
                .andExpect(jsonPath("$.paths['/api/screenings/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/csrf']").exists())
                // Verify PATCH operations are exposed under /api/screenings/{id}
                .andExpect(jsonPath("$.paths['/api/screenings/{id}'].patch").exists())
                // Verify MVC controllers are not in OpenAPI
                .andExpect(jsonPath("$.paths['/screenings']").doesNotExist())
                .andExpect(jsonPath("$.paths['/login']").doesNotExist())
                // Verify schemas
                .andExpect(jsonPath("$.components.schemas.ScreeningRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ScreeningResponse").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponse").exists())
                .andExpect(jsonPath("$.components.securitySchemes.cookieAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.csrfToken").exists());
    }

    @Test
    void swaggerUi_accessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui/index.html")));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
