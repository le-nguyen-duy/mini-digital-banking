package com.bankingdemo.transactionservice.kafka;

import com.bankingdemo.common.kafka.KafkaTopics;
import com.bankingdemo.common.kafka.TransactionCompletedEvent;
import com.bankingdemo.common.kafka.TransactionCreatedEvent;
import com.bankingdemo.common.kafka.TransactionFailedEvent;
import com.bankingdemo.transactionservice.outbox.OutboxEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Instead of calling KafkaTemplate directly (fire-and-forget, silently lost
 * if Kafka is down when the Transaction row commits), this appends events
 * to the outbox_events table in the SAME DB transaction as the write in
 * TransactionService. OutboxRelay asynchronously delivers them to Kafka
 * afterwards and retries until it succeeds - see requirement doc section 4
 * "Consistency" / dual-write problem.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final OutboxEventWriter outboxEventWriter;

    public void publishCreated(TransactionCreatedEvent event) {
        outboxEventWriter.append("Transaction", event.transactionId(), KafkaTopics.TRANSACTION_CREATED, event);
        log.info("Recorded outbox event transaction.created for transactionId {}", event.transactionId());
    }

    public void publishCompleted(TransactionCompletedEvent event) {
        outboxEventWriter.append("Transaction", event.transactionId(), KafkaTopics.TRANSACTION_COMPLETED, event);
        log.info("Recorded outbox event transaction.completed for transactionId {} (status={})",
                event.transactionId(), event.status());
    }

    public void publishFailed(TransactionFailedEvent event) {
        outboxEventWriter.append("Transaction", event.transactionId(), KafkaTopics.TRANSACTION_FAILED, event);
        log.info("Recorded outbox event transaction.failed for transactionId {} (errorCode={})",
                event.transactionId(), event.errorCode());
    }
}
