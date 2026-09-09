package com.bankingdemo.transactionservice.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response body from account-service's balance endpoints (see
 * account-service.yaml BalanceResponse schema).
 */
public record BalanceResponse(
        UUID accountId,
        BigDecimal balance,
        Instant updatedAt
) {
}
