package com.kopnus.jeremiah_recruitment_test.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

@Entity
@Immutable  // Hibernate never issues UPDATE for this entity (mirrors the DB trigger)
@Table(name = "ledger")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false)
    private Long accountNumber;

    @Column(name = "transfer_reference_number", nullable = false)
    private Long transferReferenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 6)
    private EntryType entryType;

    @Column(nullable = false)
    private long amount;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Ledger() {
        // for JPA
    }

    private Ledger(Long accountNumber, Long transferReferenceNumber, EntryType entryType,
                   long amount, long balanceAfter) {
        this.accountNumber = accountNumber;
        this.transferReferenceNumber = transferReferenceNumber;
        this.entryType = entryType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    /** Call AFTER account.debit(...) so balanceAfter is the new balance. */
    public static Ledger debit(BalanceTransfer transfer, Account originator) {
        return new Ledger(originator.getNumber(), transfer.getReferenceNumber(),
                EntryType.DEBIT, transfer.getAmount(), originator.getBalance());
    }

    /** Call AFTER account.credit(...) so balanceAfter is the new balance. */
    public static Ledger credit(BalanceTransfer transfer, Account beneficiary) {
        return new Ledger(beneficiary.getNumber(), transfer.getReferenceNumber(),
                EntryType.CREDIT, transfer.getAmount(), beneficiary.getBalance());
    }

    public Long getId() { return id; }
    public Long getAccountNumber() { return accountNumber; }
    public Long getTransferReferenceNumber() { return transferReferenceNumber; }
    public EntryType getEntryType() { return entryType; }
    public long getAmount() { return amount; }
    public long getBalanceAfter() { return balanceAfter; }
    public Instant getCreatedAt() { return createdAt; }

}
