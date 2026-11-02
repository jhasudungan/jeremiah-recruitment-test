package com.kopnus.jeremiah_recruitment_test.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "balance_transfer")
public class BalanceTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reference_number")
    private Long referenceNumber;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "originator_account_number", nullable = false)
    private Long originatorAccountNumber;

    @Column(name = "beneficiary_account_number", nullable = false)
    private Long beneficiaryAccountNumber;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(length = 255)
    private String remark;

    @CreationTimestamp
    @Column(name = "transferred_at", nullable = false, updatable = false)
    private Instant transferredAt;

    protected BalanceTransfer() {
        // for JPA
    }

    private BalanceTransfer(String idempotencyKey, Long originatorAccountNumber,
                            Long beneficiaryAccountNumber, long amount,
                            TransferStatus status, String remark) {
        this.idempotencyKey = idempotencyKey;
        this.originatorAccountNumber = originatorAccountNumber;
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
        this.amount = amount;
        this.status = status;
        this.remark = remark;
    }

    public static BalanceTransfer success(String idempotencyKey, Long originatorAccountNumber,
                                          Long beneficiaryAccountNumber, long amount, String remark) {
        return new BalanceTransfer(idempotencyKey, originatorAccountNumber,
                beneficiaryAccountNumber, amount, TransferStatus.SUCCESS, remark);
    }

    public Long getReferenceNumber() { return referenceNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getOriginatorAccountNumber() { return originatorAccountNumber; }
    public Long getBeneficiaryAccountNumber() { return beneficiaryAccountNumber; }
    public long getAmount() { return amount; }
    public TransferStatus getStatus() { return status; }
    public String getRemark() { return remark; }
    public Instant getTransferredAt() { return transferredAt; }
}
