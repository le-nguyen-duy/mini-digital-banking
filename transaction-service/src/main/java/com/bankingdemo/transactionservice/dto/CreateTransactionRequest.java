package com.bankingdemo.transactionservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(
        @NotNull(message = "fromAccount không được để trống")
        UUID fromAccount,

        @NotNull(message = "toAccount không được để trống")
        UUID toAccount,

        @NotNull(message = "amount không được để trống")
        @DecimalMin(value = "0.01", message = "amount phải >= 0.01")
        BigDecimal amount,

        @Size(max = 255, message = "description tối đa 255 ký tự")
        String description
) {
}
