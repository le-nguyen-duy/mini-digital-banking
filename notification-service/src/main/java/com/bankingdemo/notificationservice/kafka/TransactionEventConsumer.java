package com.bankingdemo.notificationservice.kafka;

import com.bankingdemo.common.kafka.TransactionCompletedEvent;
import com.bankingdemo.common.kafka.TransactionFailedEvent;
import com.bankingdemo.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the transaction lifecycle events published by transaction-service
 * (requirement doc section 8.1) and records a simulated notification for
 * each. Both listeners share the {@code notification-service-group} consumer
 * group and delegate the idempotent create logic to {@link NotificationService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "transaction.completed", groupId = "notification-service-group")
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Received transaction.completed for transactionId={}", event.transactionId());
        notificationService.recordCompleted(event);
    }

    @KafkaListener(topics = "transaction.failed", groupId = "notification-service-group")
    public void onTransactionFailed(TransactionFailedEvent event) {
        log.info("Received transaction.failed for transactionId={}", event.transactionId());
        notificationService.recordFailed(event);
    }
}
