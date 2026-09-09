package com.bankingdemo.common.kafka;

import java.math.BigDecimal;

/**
 * Published by account-service on topic {@code account.balance.updated}.
 * Consumed by transaction-service to move a transaction from PENDING to
 * COMPLETED/FAILED once the balance update has been applied.
 */
public record AccountBalanceUpdatedEvent(
        String accountId,
        BigDecimal newBalance,
        String transactionId,
        boolean success,
        String errorCode
) {
}
