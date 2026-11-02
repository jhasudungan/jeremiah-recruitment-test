package com.kopnus.jeremiah_recruitment_test.repository;


import com.kopnus.jeremiah_recruitment_test.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Loads the account and locks its row until the current transaction ends
     * (SELECT ... FOR UPDATE). Any other transaction trying to lock the same
     * account waits here, so balance updates on one account happen one at a time.
     *
     * Must be called inside a @Transactional method, otherwise the lock is
     * released immediately.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.number = :number")
    Optional<Account> findByNumberForUpdate(@Param("number") Long number);

}
