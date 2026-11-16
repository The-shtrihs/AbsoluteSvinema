package ua.kpi.absolutesvinema.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.absolutesvinema.dto.api.CsrfResponse;

@Tag(name = "Authentication & CSRF", description = "Endpoints for CSRF token acquisition and session management")
@RestController
@RequestMapping("/api/csrf")
public class CsrfRestController {

    @Operation(
            summary = "Retrieve CSRF token",
            description = "Returns the active CSRF token for the current session, along with the expected header name " +
                    "('X-CSRF-TOKEN') and parameter name ('_csrf'). Clients must send this token in the X-CSRF-TOKEN header " +
                    "for all protected state-changing operations (POST, PUT, PATCH, DELETE). Note that logging in at POST /login " +
                    "may refresh the CSRF token, so clients should re-fetch it after authenticating."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "CSRF token details retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CsrfResponse.class))
            )
    })
    @GetMapping
    public CsrfResponse getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute("_csrf");
        }
        if (csrfToken != null) {
            return new CsrfResponse(
                    csrfToken.getToken(),
                    csrfToken.getHeaderName(),
                    csrfToken.getParameterName()
            );
        }
        return new CsrfResponse("", "X-CSRF-TOKEN", "_csrf");
    }
}
