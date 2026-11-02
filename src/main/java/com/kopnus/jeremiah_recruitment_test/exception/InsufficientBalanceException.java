package com.kopnus.jeremiah_recruitment_test.exception;

public final class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(Long accountNumber) {
        super("INSUFFICIENT_BALANCE", "Insufficient balance on account " + accountNumber);
    }
}
