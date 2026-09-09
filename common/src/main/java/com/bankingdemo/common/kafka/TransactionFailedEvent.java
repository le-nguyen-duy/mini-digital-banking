package com.bankingdemo.common.kafka;

/**
 * Published by transaction-service on topic {@code transaction.failed}.
 * Consumed by notification-service.
 */
public record TransactionFailedEvent(
        String transactionId,
        String errorCode,
        String customerId
) {
}
