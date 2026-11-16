package ua.kpi.absolutesvinema.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.absolutesvinema.dto.api.CsrfResponse;

@RestController
@RequestMapping("/api/csrf")
public class CsrfRestController {

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
