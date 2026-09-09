package com.bankingdemo.accountservice.service;

import com.bankingdemo.accountservice.domain.ProcessedTransaction;
import com.bankingdemo.accountservice.kafka.AccountEventPublisher;
import com.bankingdemo.accountservice.repository.ProcessedTransactionRepository;
import com.bankingdemo.common.kafka.AccountBalanceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Persists the idempotency marker and the outbox event for a FAILED balance
 * update (e.g. insufficient balance) in its own, independent DB transaction
 * (REQUIRES_NEW).
 *
 * AccountService#updateBalance re-throws the business exception after a
 * failed operation so the caller still receives an error response — that
 * re-throw rolls back AccountService's own @Transactional method. Without
 * REQUIRES_NEW, the markProcessed row and the outbox row recording "this
 * attempt failed with X" would be rolled back right along with it, so the
 * failure would silently disappear: no idempotency guard on retry, and no
 * account.balance.updated (success=false) event for consumers. Running in a
 * brand new transaction that commits immediately — before the exception
 * propagates back to the caller — fixes that.
 *
 * This must be a separate Spring bean (not a private/self-invoked method of
 * AccountService): Spring's @Transactional relies on a proxy around the
 * bean, and a self-invocation (this.method()) bypasses that proxy, so the
 * REQUIRES_NEW annotation would silently have no effect if kept inside
 * AccountService itself.
 */
@Service
@RequiredArgsConstructor
public class FailedBalanceUpdateRecorder {

    private final ProcessedTransactionRepository processedTransactionRepository;
    private final AccountEventPublisher accountEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(UUID accountId, String transactionId, BigDecimal currentBalance, String errorCode) {
        ProcessedTransaction processed = new ProcessedTransaction();
        processed.setAccountId(accountId);
        processed.setTransactionId(transactionId);
        processedTransactionRepository.save(processed);

        accountEventPublisher.publishBalanceUpdated(new AccountBalanceUpdatedEvent(
                accountId.toString(),
                currentBalance,
                transactionId,
                false,
                errorCode
        ));
    }
}
