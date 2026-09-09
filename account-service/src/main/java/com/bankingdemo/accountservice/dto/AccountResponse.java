package com.bankingdemo.accountservice.dto;

import com.bankingdemo.accountservice.domain.Account;
import com.bankingdemo.accountservice.domain.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String accountNumber,
        String ownerUserId,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerUserId(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
