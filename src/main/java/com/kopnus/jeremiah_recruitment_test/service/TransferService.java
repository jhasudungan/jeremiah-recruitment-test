package com.kopnus.jeremiah_recruitment_test.service;

import com.kopnus.jeremiah_recruitment_test.dto.TransferRequest;
import com.kopnus.jeremiah_recruitment_test.dto.TransferResponse;
import com.kopnus.jeremiah_recruitment_test.dto.TransferResult;
import com.kopnus.jeremiah_recruitment_test.entity.Account;
import com.kopnus.jeremiah_recruitment_test.entity.BalanceTransfer;
import com.kopnus.jeremiah_recruitment_test.entity.Ledger;
import com.kopnus.jeremiah_recruitment_test.exception.*;
import com.kopnus.jeremiah_recruitment_test.repository.AccountRepository;
import com.kopnus.jeremiah_recruitment_test.repository.BalanceTransferRepository;
import com.kopnus.jeremiah_recruitment_test.repository.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final BalanceTransferRepository transferRepository;
    private final LedgerRepository ledgerRepository;

    public TransferService(AccountRepository accountRepository,
                           BalanceTransferRepository transferRepository,
                           LedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.ledgerRepository = ledgerRepository;
    }

    private Account lockAccount(Long accountNumber) {
        return accountRepository.findByNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    /**
     * Transfers money between two accounts in a single database transaction.
     *
     * @param userId         authenticated user (from the JWT), must own the originator account
     * @param idempotencyKey client-supplied key; the same key returns the original result
     */
    @Transactional
    public TransferResult transfer(Long userId, String idempotencyKey, TransferRequest request) {

        Long fromNumber = request.originatorAccountNumber();
        Long toNumber = request.beneficiaryAccountNumber();
        long amount = request.amount();

        // 1. Easy validation before touching the database
        if (fromNumber.equals(toNumber)) {
            throw new InvalidTransferException("Originator and beneficiary must be different accounts");
        }

        // 2. Lock both accounts, always in ascending account-number order (prevents deadlocks)
        Account originator;
        Account beneficiary;
        if (fromNumber < toNumber) {
            originator = lockAccount(fromNumber);
            beneficiary = lockAccount(toNumber);
        } else {
            beneficiary = lockAccount(toNumber);
            originator = lockAccount(fromNumber);
        }

        // 3. Authorization: the caller must own the originator account
        if (!originator.isOwnedBy(userId)) {
            throw new AccountAccessDeniedException(fromNumber);
        }

        // 4. Idempotency: checked AFTER locking, so a concurrent duplicate request
        //    waits for the first one to commit and then sees its result here
        Optional<BalanceTransfer> existing =
                transferRepository.findByOriginatorAccountNumberAndIdempotencyKey(fromNumber, idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        // 5. Business rules
        if (!originator.isActive()) {
            throw new AccountInactiveException(fromNumber);
        }
        if (!beneficiary.isActive()) {
            throw new AccountInactiveException(toNumber);
        }
        if (originator.getBalance() < amount) {
            throw new InsufficientBalanceException(fromNumber);
        }

        // 6. Move the money (dirty checking writes the new balances at commit)
        originator.debit(amount);
        beneficiary.credit(amount);

        // 7. Record the transfer and post both ledger entries
        BalanceTransfer transfer = transferRepository.save(
                BalanceTransfer.success(idempotencyKey, fromNumber, toNumber, amount, request.remark()));
        ledgerRepository.save(Ledger.debit(transfer, originator));
        ledgerRepository.save(Ledger.credit(transfer, beneficiary));

        return TransferResult.created(TransferResponse.from(transfer));
    }

    /**
     * Same key + same payload  -> return the original result (no second transfer).
     * Same key + different payload -> client bug, reject it.
     */
    private TransferResult replay(BalanceTransfer existing, TransferRequest request) {
        boolean samePayload =
                existing.getBeneficiaryAccountNumber().equals(request.beneficiaryAccountNumber())
                        && existing.getAmount() == request.amount();
        if (!samePayload) {
            throw new IdempotencyKeyConflictException(existing.getIdempotencyKey());
        }
        return TransferResult.replayed(TransferResponse.from(existing));
    }

}
