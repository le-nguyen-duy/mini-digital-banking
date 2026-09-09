package com.bankingdemo.common.kafka;

/**
 * Central registry of Kafka topic names (requirement doc section 8.1).
 */
public final class KafkaTopics {

    public static final String TRANSACTION_CREATED = "transaction.created";
    public static final String ACCOUNT_BALANCE_UPDATED = "account.balance.updated";
    public static final String TRANSACTION_COMPLETED = "transaction.completed";
    public static final String TRANSACTION_FAILED = "transaction.failed";

    private KafkaTopics() {
    }
}
