package com.kopnus.jeremiah_recruitment_test.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "account")
public class Account {

    @Id
    private Long number;  // assigned account number, not generated

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private long balance;

    @Version
    private long version;

    // Owned by the database (DEFAULT now() + trigger), so Hibernate never writes them
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated", insertable = false, updatable = false)
    private Instant lastUpdated;

    protected Account() {
        // for JPA
    }

    // ---- Domain behaviour ----
    public void debit(long amount) {
        requirePositive(amount);
        if (balance < amount) {
            throw new IllegalStateException("Insufficient balance on account " + number);
        }
        balance -= amount;
    }

    public void credit(long amount) {
        requirePositive(amount);
        balance = Math.addExact(balance, amount); // throws on overflow instead of wrapping
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    // ---- Getters ----

    public Long getNumber() { return number; }
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public boolean isActive() { return active; }
    public long getBalance() { return balance; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUpdated() { return lastUpdated; }

}
