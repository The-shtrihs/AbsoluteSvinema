package ua.kpi.absolutesvinema.dto.api;

public record CsrfResponse(
        String token,
        String headerName,
        String parameterName
) {
}
