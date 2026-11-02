package com.kopnus.jeremiah_recruitment_test.controller;

import com.kopnus.jeremiah_recruitment_test.dto.ApiError;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiError> handleError(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = code instanceof Integer value ? HttpStatus.resolve(value) : null;
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Never expose internal details for server errors
        if (status.is5xxServerError()) {
            return ResponseEntity.status(status)
                    .body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred"));
        }

        return ResponseEntity.status(status)
                .body(ApiError.of(status.name(), status.getReasonPhrase()));
    }


}
