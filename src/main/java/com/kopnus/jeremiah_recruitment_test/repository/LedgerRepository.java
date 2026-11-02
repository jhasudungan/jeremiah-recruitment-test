package com.kopnus.jeremiah_recruitment_test.repository;

import com.kopnus.jeremiah_recruitment_test.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

    List<Ledger> findByTransferReferenceNumberOrderByIdAsc(Long transferReferenceNumber);

}
