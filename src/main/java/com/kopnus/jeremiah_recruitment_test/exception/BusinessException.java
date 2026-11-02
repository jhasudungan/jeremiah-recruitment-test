package com.kopnus.jeremiah_recruitment_test.exception;

public abstract sealed class BusinessException extends RuntimeException
        permits AccountNotFoundException,
        AccountInactiveException,
        AccountAccessDeniedException,
        InsufficientBalanceException,
        InvalidTransferException,
        IdempotencyKeyConflictException,
        InvalidCredentialsException {

    private final String errorCode;

    protected BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}