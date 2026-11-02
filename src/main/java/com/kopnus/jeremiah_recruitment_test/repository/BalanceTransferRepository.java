package com.kopnus.jeremiah_recruitment_test.repository;


import com.kopnus.jeremiah_recruitment_test.entity.BalanceTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BalanceTransferRepository extends JpaRepository<BalanceTransfer, Long> {

    // Duplicate-request check: has this originator already used this key?
    Optional<BalanceTransfer> findByOriginatorAccountNumberAndIdempotencyKey(
            Long originatorAccountNumber, String idempotencyKey);
}
