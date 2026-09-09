package com.bankingdemo.transactionservice.dto;

import com.bankingdemo.transactionservice.domain.Transaction;
import com.bankingdemo.transactionservice.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID fromAccount,
        UUID toAccount,
        BigDecimal amount,
        TransactionStatus status,
        String errorCode,
        Instant createdAt,
        Instant updatedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccount(),
                transaction.getToAccount(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getErrorCode(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
