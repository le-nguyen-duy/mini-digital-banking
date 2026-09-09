package com.bankingdemo.common.kafka;

/**
 * Published by transaction-service on topic {@code transaction.completed}.
 * Consumed by notification-service.
 */
public record TransactionCompletedEvent(
        String transactionId,
        String status,
        String customerId
) {
}
