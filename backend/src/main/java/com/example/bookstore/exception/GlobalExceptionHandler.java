package com.example.bookstore.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every exception into an RFC 7807 ProblemDetail. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String TYPE_BASE = "https://bookstore.example/problems/";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<Integer, String> FRAMEWORK_TYPES = Map.of(
            405, "method-not-allowed",
            415, "unsupported-media-type");

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found", ex.getMessage(), request);
    }

    /** Missing or wrong credentials. The detail never says which, so accounts cannot be probed. */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "unauthorized",
                "Unauthorized",
                "Authentication is required, or the credentials are invalid",
                request);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT, "email-already-registered", "Email already registered", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        ProblemDetail body =
                problem(HttpStatus.CONFLICT, "insufficient-stock", "Insufficient stock", ex.getMessage(), request);
        body.setProperty("errors", ex.getShortages());
        return body;
    }

    @ExceptionHandler(InvalidQuantityException.class)
    ProblemDetail handleInvalidQuantity(InvalidQuantityException ex, HttpServletRequest request) {
        return validationProblem(List.of(new FieldErrorResponse("quantity", ex.getMessage())), request);
    }

    @ExceptionHandler(EmptyCartException.class)
    ProblemDetail handleEmptyCart(EmptyCartException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "cart-empty", "Cart is empty", ex.getMessage(), request);
    }

    /**
     * A concurrent change (version conflict, unique-key race, lock failure). The transaction was rolled back, so a
     * retry is safe. Inputs are validated first, so a constraint violation reaching the database means a race.
     */
    @ExceptionHandler({
        ObjectOptimisticLockingFailureException.class,
        ConcurrentRequestException.class,
        PessimisticLockingFailureException.class,
        DataIntegrityViolationException.class
    })
    ProblemDetail handleConcurrentModification(RuntimeException ex, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "concurrent-modification",
                "Concurrent modification",
                "The data was changed by another request. Please reload and try again.",
                request);
    }

    @ExceptionHandler(InvalidRequestParameterException.class)
    ProblemDetail handleInvalidParameter(InvalidRequestParameterException ex, HttpServletRequest request) {
        return validationProblem(List.of(new FieldErrorResponse(ex.getParameter(), ex.getMessage())), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error",
                "Internal server error",
                "An unexpected error occurred",
                request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldErrorResponse(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors, servletRequest(request)));
    }

    /** Constraint violations on @RequestParam / @PathVariable (e.g. size > 100). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorResponse(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors, servletRequest(request)));
    }

    private static ProblemDetail validationProblem(List<FieldErrorResponse> errors, HttpServletRequest request) {
        ProblemDetail body = problem(
                HttpStatus.BAD_REQUEST,
                "validation-error",
                "Validation failed",
                "One or more fields are invalid",
                request);
        body.setProperty(
                "errors",
                errors.stream()
                        .sorted(Comparator.comparing(FieldErrorResponse::field))
                        .toList());
        return body;
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(problem(
                        HttpStatus.BAD_REQUEST,
                        "malformed-request",
                        "Malformed request",
                        "Request body is missing or not valid JSON",
                        servletRequest(request)));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getPropertyName());
        return ResponseEntity.badRequest()
                .body(problem(
                        HttpStatus.BAD_REQUEST,
                        "malformed-request",
                        "Malformed request",
                        detail,
                        servletRequest(request)));
    }

    /** Unknown URL: same shape as our own 404s, without Spring's "No static resource" wording. */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpServletRequest servletRequest = servletRequest(request);
        String detail = "No endpoint %s %s".formatted(servletRequest.getMethod(), servletRequest.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found", detail, servletRequest));
    }

    /** Gives framework-generated problems (405, 415, ...) the same shape as our own. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null
                && response.getBody() instanceof ProblemDetail problemDetail
                && isBlankType(problemDetail)) {
            String slug = FRAMEWORK_TYPES.getOrDefault(statusCode.value(), "error");
            problemDetail.setType(URI.create(TYPE_BASE + slug));
            problemDetail.setInstance(URI.create(servletRequest(request).getRequestURI()));
            problemDetail.setProperty("timestamp", Instant.now());
        }
        return response;
    }

    protected static ProblemDetail problem(
            HttpStatus status, String typeSlug, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + typeSlug));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private static boolean isBlankType(ProblemDetail problem) {
        return problem.getType() == null
                || "about:blank".equals(problem.getType().toString());
    }

    private static HttpServletRequest servletRequest(WebRequest request) {
        return ((ServletWebRequest) request).getRequest();
    }

    record FieldErrorResponse(String field, String message) {}
}
