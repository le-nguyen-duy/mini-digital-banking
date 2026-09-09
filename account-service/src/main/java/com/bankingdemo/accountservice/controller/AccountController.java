package com.bankingdemo.accountservice.controller;

import com.bankingdemo.accountservice.dto.AccountResponse;
import com.bankingdemo.accountservice.dto.BalanceResponse;
import com.bankingdemo.accountservice.dto.CreateAccountRequest;
import com.bankingdemo.accountservice.dto.UpdateBalanceRequest;
import com.bankingdemo.accountservice.security.AccountAccessGuard;
import com.bankingdemo.accountservice.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountAccessGuard accountAccessGuard;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TELLER')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId, Authentication authentication) {
        AccountResponse response = accountService.getAccount(accountId);
        accountAccessGuard.checkAccess(authentication, response.ownerUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID accountId, Authentication authentication) {
        AccountResponse account = accountService.getAccount(accountId);
        accountAccessGuard.checkAccess(authentication, account.ownerUserId());
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    /**
     * Internal endpoint, intended to be called only by transaction-service
     * (client-credentials token). Idempotent via X-Transaction-Id header.
     */
    @PatchMapping("/{accountId}/balance")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<BalanceResponse> updateBalance(
            @PathVariable UUID accountId,
            @RequestHeader("X-Transaction-Id") @NotBlank String transactionId,
            @Valid @RequestBody UpdateBalanceRequest request) {
        BalanceResponse response = accountService.updateBalance(accountId, transactionId, request);
        return ResponseEntity.ok(response);
    }
}
