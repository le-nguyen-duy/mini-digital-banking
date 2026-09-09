package com.bankingdemo.common.kafka;

import java.math.BigDecimal;

/**
 * Published by transaction-service on topic {@code transaction.created}.
 * Consumed by account-service to debit/credit the involved accounts.
 */
public record TransactionCreatedEvent(
        String transactionId,
        String fromAccount,
        String toAccount,
        BigDecimal amount
) {
}
