package ua.kpi.absolutesvinema.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AbsoluteSvinema REST API")
                        .version("1.0.0")
                        .description("""
                                RESTful web service API for AbsoluteSvinema cinema ticket booking system (Lab 4).
                                Provides CRUD operations, filtering, pagination, and partial updates (RFC 6902 JSON Patch & RFC 7396 JSON Merge Patch)
                                for Screening resources.
                                
                                ### Authentication & Security
                                - **Public operations:** GET /api/csrf, GET /api/screenings, GET /api/screenings/{id}.
                                - **Protected operations:** POST, PUT, PATCH, DELETE on /api/screenings require **ROLE_ADMIN**,
                                  an active HTTP session cookie (`JSESSIONID`), and a valid CSRF token header (`X-CSRF-TOKEN`).
                                """)
                        .contact(new Contact()
                                .name("AbsoluteSvinema Team")
                                .email("admin@svinema.ua")))
                .components(new Components()
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Session cookie obtained via form login at POST /login"))
                        .addSecuritySchemes("csrfToken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-CSRF-TOKEN")
                                .description("CSRF token obtained via GET /api/csrf, required on state-changing requests")));
    }

    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("screenings-api")
                .displayName("Screenings API (Lab 4)")
                .pathsToMatch("/api/**")
                .packagesToScan("ua.kpi.absolutesvinema.controller.api")
                .build();
    }
}
