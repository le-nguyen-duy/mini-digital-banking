package com.bankingdemo.transactionservice.client;

import java.math.BigDecimal;

/**
 * Request body for account-service's PATCH /api/v1/accounts/{accountId}/balance
 * endpoint (see account-service.yaml UpdateBalanceRequest schema).
 */
public record UpdateBalanceRequest(
        BigDecimal amount,
        BalanceOperation operation,
        String transactionId
) {
}
