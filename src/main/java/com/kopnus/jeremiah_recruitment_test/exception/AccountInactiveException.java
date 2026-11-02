package com.kopnus.jeremiah_recruitment_test.exception;

public final class AccountInactiveException extends BusinessException {
    public AccountInactiveException(Long accountNumber) {
        super("ACCOUNT_INACTIVE", "Account " + accountNumber + " is not active");
    }
}
