package com.kopnus.jeremiah_recruitment_test.dto;

import com.kopnus.jeremiah_recruitment_test.entity.BalanceTransfer;
import com.kopnus.jeremiah_recruitment_test.entity.TransferStatus;

import java.time.Instant;

public record TransferResponse(
        Long referenceNumber,
        Long originatorAccountNumber,
        Long beneficiaryAccountNumber,
        long amount,
        TransferStatus status,
        String remark,
        Instant transferredAt
) {

    public static TransferResponse from(BalanceTransfer transfer) {
        return new TransferResponse(
                transfer.getReferenceNumber(),
                transfer.getOriginatorAccountNumber(),
                transfer.getBeneficiaryAccountNumber(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getRemark(),
                transfer.getTransferredAt());
    }
}
