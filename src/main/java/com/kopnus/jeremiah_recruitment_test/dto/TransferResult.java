package com.kopnus.jeremiah_recruitment_test.dto;

public record TransferResult (TransferResponse transfer, boolean replayed) {

    public static TransferResult created(TransferResponse transfer) {
        return new TransferResult(transfer, false);
    }

    public static TransferResult replayed(TransferResponse transfer) {
        return new TransferResult(transfer, true);
    }
}
