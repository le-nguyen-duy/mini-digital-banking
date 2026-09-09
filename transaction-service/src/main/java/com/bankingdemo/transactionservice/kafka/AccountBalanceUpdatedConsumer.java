package com.bankingdemo.transactionservice.kafka;

import com.bankingdemo.common.kafka.AccountBalanceUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to {@code account.balance.updated}, published by account-service
 * after it applies a debit/credit.
 *
 * NOTE: this demo drives the transaction state machine (PENDING -> COMPLETED
 * / FAILED) synchronously from {@code TransactionService}, based on the
 * direct REST response of the debit/credit calls to account-service. This
 * Kafka event is therefore NOT used to transition transaction state — it is
 * consumed purely for audit trail / observability purposes (and as a hook
 * for future extensions, e.g. driving notification triggers independently
 * of the synchronous flow). Keeping the primary flow purely REST-driven
 * keeps the demo's core business logic easy to reason about, while this
 * listener demonstrates a working, idempotent Kafka consumer pattern.
 */
@Component
@Slf4j
public class AccountBalanceUpdatedConsumer {

    @KafkaListener(topics = "account.balance.updated", groupId = "transaction-service-group")
    public void onAccountBalanceUpdated(AccountBalanceUpdatedEvent event) {
        log.info("Audit: account.balance.updated received - accountId={}, transactionId={}, newBalance={}, success={}, errorCode={}",
                event.accountId(), event.transactionId(), event.newBalance(), event.success(), event.errorCode());
    }
}
