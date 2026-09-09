package com.bankingdemo.accountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "ownerUserId không được để trống")
        String ownerUserId,

        @NotNull(message = "initialBalance không được để trống")
        @DecimalMin(value = "0", inclusive = true, message = "initialBalance phải >= 0")
        BigDecimal initialBalance
) {
}
