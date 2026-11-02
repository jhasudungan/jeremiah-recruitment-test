package com.kopnus.jeremiah_recruitment_test.controller;

import com.kopnus.jeremiah_recruitment_test.config.RateLimitProperties;
import com.kopnus.jeremiah_recruitment_test.dto.TransferRequest;
import com.kopnus.jeremiah_recruitment_test.dto.TransferResponse;
import com.kopnus.jeremiah_recruitment_test.dto.TransferResult;
import com.kopnus.jeremiah_recruitment_test.security.AuthenticatedUser;
import com.kopnus.jeremiah_recruitment_test.security.RateLimiter;
import com.kopnus.jeremiah_recruitment_test.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final TransferService transferService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public TransferController(TransferService transferService,
                              RateLimiter rateLimiter,
                              RateLimitProperties rateLimitProperties) {

        this.transferService = transferService;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(IDEMPOTENCY_HEADER) @NotBlank @Size(max = 64) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        long remaining = rateLimiter.consume("transfer:user:" + user.id(), rateLimitProperties.transfer());
        TransferResult result = transferService.transfer(user.id(), idempotencyKey, request);
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(status)
                .header(REPLAYED_HEADER, String.valueOf(result.replayed()))
                .header(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining))
                .body(result.transfer());

    }
}
