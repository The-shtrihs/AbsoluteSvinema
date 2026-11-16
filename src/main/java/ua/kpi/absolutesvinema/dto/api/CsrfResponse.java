package ua.kpi.absolutesvinema.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CSRF token details for client requests")
public record CsrfResponse(
        @Schema(description = "CSRF token value to be passed in HTTP header", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String token,

        @Schema(description = "HTTP header name for CSRF token", example = "X-CSRF-TOKEN")
        String headerName,

        @Schema(description = "Form parameter name for CSRF token", example = "_csrf")
        String parameterName
) {
}
