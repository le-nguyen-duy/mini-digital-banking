package com.bankingdemo.transactionservice.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
