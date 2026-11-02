package com.kopnus.jeremiah_recruitment_test.exception;

public final class AccountAccessDeniedException extends BusinessException {
    public AccountAccessDeniedException(Long accountNumber) {
        super("ACCOUNT_ACCESS_DENIED", "You are not allowed to transfer from account " + accountNumber);
    }
}
