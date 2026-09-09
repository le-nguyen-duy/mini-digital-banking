package com.bankingdemo.accountservice.kafka;

import com.bankingdemo.accountservice.outbox.OutboxEventWriter;
import com.bankingdemo.common.kafka.AccountBalanceUpdatedEvent;
import com.bankingdemo.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Instead of calling KafkaTemplate directly (fire-and-forget, silently lost
 * if Kafka is down when the balance update commits), this appends the event
 * to the outbox_events table in the SAME DB transaction as the balance
 * write in AccountService#updateBalance. OutboxRelay asynchronously
 * delivers it to Kafka afterwards and retries until it succeeds - see
 * requirement doc section 4 "Consistency" / dual-write problem.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventPublisher {

    private final OutboxEventWriter outboxEventWriter;

    public void publishBalanceUpdated(AccountBalanceUpdatedEvent event) {
        outboxEventWriter.append("Account", event.accountId(), KafkaTopics.ACCOUNT_BALANCE_UPDATED, event);
        log.info("Recorded outbox event account.balance.updated for account {} (transactionId={}, success={})",
                event.accountId(), event.transactionId(), event.success());
    }
}
