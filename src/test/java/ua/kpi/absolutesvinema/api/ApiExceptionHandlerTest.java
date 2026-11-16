package ua.kpi.absolutesvinema.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ua.kpi.absolutesvinema.controller.api.ApiExceptionHandler;
import ua.kpi.absolutesvinema.dto.api.ScreeningRequest;
import ua.kpi.absolutesvinema.exception.ResourceNotFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
    }

    @Test
    void handleNotFoundReturns404ProblemDetail() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Screening", 99L);
        ProblemDetail problem = handler.handleNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getDetail()).contains("Screening not found: 99");
    }

    @Test
    void handleValidationErrorsReturns400WithErrorsMap() throws Exception {
        ScreeningRequest target = new ScreeningRequest("", null, null, null, "", null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "screeningRequest");
        bindingResult.addError(new FieldError("screeningRequest", "movieTitle", "Movie title must not be blank"));
        bindingResult.addError(new FieldError("screeningRequest", "hall", "Hall must not be blank"));

        var method = getClass().getDeclaredMethod("setUp");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(method, -1), bindingResult);

        ProblemDetail problem = handler.handleValidationErrors(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Request Content");
        assertThat(problem.getDetail()).isEqualTo("Validation failed");

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors)
                .containsEntry("movieTitle", "Movie title must not be blank")
                .containsEntry("hall", "Hall must not be blank");
    }

    @Test
    void handleMessageNotReadableReturns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid JSON", (org.springframework.http.HttpInputMessage) null);
        ProblemDetail problem = handler.handleMessageNotReadable(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Malformed Request Payload");
        assertThat(problem.getDetail()).isEqualTo("Malformed JSON request body");
    }

    @Test
    void handleIllegalStateReturns409Conflict() {
        IllegalStateException ex = new IllegalStateException("Seat is already booked");
        ProblemDetail problem = handler.handleIllegalState(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("Seat is already booked");
    }

    @Test
    void handleIllegalArgumentReturns400BadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid ID argument");
        ProblemDetail problem = handler.handleIllegalArgument(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        assertThat(problem.getDetail()).isEqualTo("Invalid ID argument");
    }

    @Test
    void handleGeneralErrorReturns500WithoutLeakingDetails() {
        Exception ex = new RuntimeException("Sensitive database stacktrace");
        ProblemDetail problem = handler.handleGeneralError(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getDetail()).doesNotContain("Sensitive database stacktrace");
    }
}
