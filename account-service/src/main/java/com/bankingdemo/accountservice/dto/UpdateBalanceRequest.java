package com.bankingdemo.accountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBalanceRequest(
        @NotNull(message = "amount không được để trống")
        @DecimalMin(value = "0.01", message = "amount phải > 0")
        BigDecimal amount,

        @NotNull(message = "operation không được để trống")
        BalanceOperation operation,

        String transactionId
) {
}
