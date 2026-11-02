package com.kopnus.jeremiah_recruitment_test.exception;

import com.kopnus.jeremiah_recruitment_test.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- 1. Business rule violations (thrown by our services) ----

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        // Exhaustive switch over the sealed hierarchy: no default needed,
        // and a new exception without a mapping is a compile error.
        HttpStatus status = switch (ex) {
            case InvalidTransferException e -> HttpStatus.BAD_REQUEST;
            case InvalidCredentialsException e -> HttpStatus.UNAUTHORIZED;
            case AccountAccessDeniedException e -> HttpStatus.FORBIDDEN;
            case AccountNotFoundException e -> HttpStatus.NOT_FOUND;
            case IdempotencyKeyConflictException e -> HttpStatus.CONFLICT;
            case AccountInactiveException e -> HttpStatus.UNPROCESSABLE_CONTENT;
            case InsufficientBalanceException e -> HttpStatus.UNPROCESSABLE_CONTENT;
        };

        log.info("Request rejected: {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    // ---- 2. Invalid input ----

    /** @Valid on the request body failed (e.g. amount is negative). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_FAILED", "Request validation failed", details));
    }

    /** Constraints on headers / path / query params failed (e.g. blank Idempotency-Key). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException ex) {
        List<String> details = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName()
                                + ": " + error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_FAILED", "Request validation failed", details));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("MISSING_HEADER", "Required header '" + ex.getHeaderName() + "' is missing"));
    }

    /** Malformed JSON, or wrong types such as "amount": "abc". */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of("MALFORMED_REQUEST", "Request body is missing or malformed"));
    }

    // ---- 3. Database safety nets ----

    /** A constraint fired that application logic should have prevented (e.g. the unique idempotency key). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("CONFLICT", "The request conflicts with the current state of the data"));
    }

    /** Optimistic (@Version) or pessimistic lock failure: safe for the client to retry. */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiError> handleConcurrency(ConcurrencyFailureException ex) {
        log.warn("Concurrency failure", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("CONCURRENT_UPDATE", "The account was modified concurrently, please retry"));
    }

    // ---- 4. Rate Limiting ----
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ApiError.of("RATE_LIMIT_EXCEEDED", ex.getMessage()));
    }

    // ---- 5. Everything else ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // Spring's own web exceptions (404 no route, 405 wrong method, 415 wrong
        // content type...) already know their status: keep it, don't turn them into 500.
        if (ex instanceof ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            String code = resolved != null ? resolved.name() : "REQUEST_ERROR";
            String detail = springError.getBody().getDetail();
            return ResponseEntity.status(status)
                    .body(ApiError.of(code, detail != null ? detail : ex.getMessage()));
        }

        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred"));
    }



}
