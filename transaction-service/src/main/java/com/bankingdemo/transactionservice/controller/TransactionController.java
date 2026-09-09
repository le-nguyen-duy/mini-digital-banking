package com.bankingdemo.transactionservice.controller;

import com.bankingdemo.transactionservice.domain.TransactionStatus;
import com.bankingdemo.transactionservice.dto.CreateTransactionRequest;
import com.bankingdemo.transactionservice.dto.TransactionPageResponse;
import com.bankingdemo.transactionservice.dto.TransactionResponse;
import com.bankingdemo.transactionservice.service.TransactionService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @RateLimiter(name = "transactionApi")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            Authentication authentication) {
        String customerId = extractSubject(authentication);
        TransactionResponse response = transactionService.createTransaction(request, idempotencyKey, customerId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.getTransaction(transactionId));
    }

    @GetMapping
    public ResponseEntity<TransactionPageResponse> getTransactionHistory(
            @RequestParam UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus status) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountId, page, size, status));
    }

    private String extractSubject(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return authentication.getName();
    }
}
