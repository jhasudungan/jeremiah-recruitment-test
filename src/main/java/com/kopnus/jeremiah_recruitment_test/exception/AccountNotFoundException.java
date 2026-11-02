package com.kopnus.jeremiah_recruitment_test.exception;

public final class AccountNotFoundException extends BusinessException {
    public AccountNotFoundException(Long accountNumber) {
        super("ACCOUNT_NOT_FOUND", "Account " + accountNumber + " not found");
    }
}
