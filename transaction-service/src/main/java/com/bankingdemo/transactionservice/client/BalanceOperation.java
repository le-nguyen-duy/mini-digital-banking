package com.bankingdemo.transactionservice.client;

/**
 * Mirrors account-service's UpdateBalanceRequest.operation enum
 * (see account-service.yaml UpdateBalanceRequest schema).
 */
public enum BalanceOperation {
    DEBIT,
    CREDIT
}
