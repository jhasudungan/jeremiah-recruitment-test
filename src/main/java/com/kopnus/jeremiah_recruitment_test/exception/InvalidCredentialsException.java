package com.kopnus.jeremiah_recruitment_test.exception;


public final class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid username or password");
    }

}
