package com.kopnus.jeremiah_recruitment_test.exception;

public final class InvalidTransferException extends BusinessException {

    public InvalidTransferException(String message) {
        super("INVALID_TRANSFER", message);
    }

}
