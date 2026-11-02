package com.kopnus.jeremiah_recruitment_test.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferRequest (
        @NotNull Long originatorAccountNumber,
        @NotNull Long beneficiaryAccountNumber,
        @NotNull @Positive Long amount,
        @Size(max = 255) String remark
) {}
