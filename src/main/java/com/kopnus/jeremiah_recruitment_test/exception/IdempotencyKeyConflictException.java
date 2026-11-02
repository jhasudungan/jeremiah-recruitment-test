package com.kopnus.jeremiah_recruitment_test.exception;

public final class IdempotencyKeyConflictException extends BusinessException {
    public IdempotencyKeyConflictException(String idempotencyKey) {
        super("IDEMPOTENCY_KEY_REUSED",
                "Idempotency key '" + idempotencyKey + "' was already used for a different transfer");
    }
}
