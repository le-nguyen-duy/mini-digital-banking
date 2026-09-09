package com.bankingdemo.accountservice.service;

import com.bankingdemo.accountservice.domain.Account;
import com.bankingdemo.accountservice.domain.AccountStatus;
import com.bankingdemo.accountservice.domain.ProcessedTransaction;
import com.bankingdemo.accountservice.dto.*;
import com.bankingdemo.accountservice.kafka.AccountEventPublisher;
import com.bankingdemo.accountservice.repository.AccountRepository;
import com.bankingdemo.accountservice.repository.ProcessedTransactionRepository;
import com.bankingdemo.common.exception.AccountInactiveException;
import com.bankingdemo.common.exception.AccountNotFoundException;
import com.bankingdemo.common.exception.InsufficientBalanceException;
import com.bankingdemo.common.kafka.AccountBalanceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final ProcessedTransactionRepository processedTransactionRepository;
    private final AccountEventPublisher accountEventPublisher;
    private final FailedBalanceUpdateRecorder failedBalanceUpdateRecorder;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account();
        account.setOwnerUserId(request.ownerUserId());
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(request.initialBalance());
        account.setStatus(AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);
        log.info("Created account {} for owner {}", saved.getId(), saved.getOwnerUserId());
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
        return new BalanceResponse(account.getId(), account.getBalance(), Instant.now());
    }

    /**
     * Applies a DEBIT/CREDIT operation to an account's balance.
     * Idempotent: if the same transactionId (from X-Transaction-Id header)
     * was already processed for this account, returns the current balance
     * without re-applying the operation (requirement doc section 2.4 / 8.2).
     */
    @Transactional
    public BalanceResponse updateBalance(UUID accountId, String transactionId, UpdateBalanceRequest request) {
        var alreadyProcessed = processedTransactionRepository
                .findByAccountIdAndTransactionId(accountId, transactionId);
        if (alreadyProcessed.isPresent()) {
            log.info("Balance update for account {} / transactionId {} already processed, skipping (idempotent)",
                    accountId, transactionId);
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));
            return new BalanceResponse(account.getId(), account.getBalance(), Instant.now());
        }

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(accountId.toString());
        }

        try {
            applyOperation(account, request);
        } catch (InsufficientBalanceException ex) {
            // Committed in its own REQUIRES_NEW transaction: the throw below
            // rolls back THIS method's transaction (balance change - there
            // is none here anyway - and any pending writes), but the
            // idempotency marker and the account.balance.updated
            // (success=false) outbox event must survive so retries are
            // still idempotent and consumers still learn about the failed
            // attempt. See FailedBalanceUpdateRecorder for details.
            failedBalanceUpdateRecorder.recordFailedAttempt(
                    accountId, transactionId, account.getBalance(), ex.getErrorCode().name());
            throw ex;
        }

        accountRepository.save(account);
        markProcessed(accountId, transactionId);
        publishBalanceUpdated(account, transactionId);

        return new BalanceResponse(account.getId(), account.getBalance(), Instant.now());
    }

    private void applyOperation(Account account, UpdateBalanceRequest request) {
        BigDecimal amount = request.amount();
        switch (request.operation()) {
            case DEBIT -> {
                if (account.getBalance().compareTo(amount) < 0) {
                    throw new InsufficientBalanceException(account.getId().toString());
                }
                account.setBalance(account.getBalance().subtract(amount));
            }
            case CREDIT -> account.setBalance(account.getBalance().add(amount));
        }
    }

    private void markProcessed(UUID accountId, String transactionId) {
        ProcessedTransaction processed = new ProcessedTransaction();
        processed.setAccountId(accountId);
        processed.setTransactionId(transactionId);
        processedTransactionRepository.save(processed);
    }

    private void publishBalanceUpdated(Account account, String transactionId) {
        accountEventPublisher.publishBalanceUpdated(new AccountBalanceUpdatedEvent(
                account.getId().toString(),
                account.getBalance(),
                transactionId,
                true,
                null
        ));
    }

    private String generateAccountNumber() {
        // 10-digit numeric account number for demo purposes.
        long number = 1_000_000_000L + Math.abs(ThreadLocalRandom.current().nextLong() % 9_000_000_000L);
        return String.valueOf(number);
    }
}
