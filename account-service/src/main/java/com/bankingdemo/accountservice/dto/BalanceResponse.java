package com.bankingdemo.accountservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceResponse(
        UUID accountId,
        BigDecimal balance,
        Instant updatedAt
) {
}
