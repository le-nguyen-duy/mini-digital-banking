package com.bankingdemo.accountservice.outbox;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
