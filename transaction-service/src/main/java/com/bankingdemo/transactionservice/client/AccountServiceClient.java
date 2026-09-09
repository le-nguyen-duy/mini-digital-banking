package com.bankingdemo.transactionservice.client;

import com.bankingdemo.common.exception.AccountInactiveException;
import com.bankingdemo.common.exception.AccountNotFoundException;
import com.bankingdemo.common.exception.BusinessException;
import com.bankingdemo.common.exception.ErrorCode;
import com.bankingdemo.common.exception.InsufficientBalanceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST client for account-service's internal balance-update endpoint
 * (PATCH /api/v1/accounts/{accountId}/balance), protected by Resilience4j
 * Circuit Breaker + Retry (requirement doc section 6).
 *
 * Only transport-level failures (timeouts, connection errors -
 * ResourceAccessException/IOException) are retried; 4xx business errors are
 * mapped to the corresponding common exceptions and are excluded from retry
 * via resilience4j.retry.instances.accountService.ignoreExceptions
 * (application.yml).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClient {

    private final RestClient accountServiceRestClient;

    @CircuitBreaker(name = "accountService", fallbackMethod = "accountServiceFallback")
    @Retry(name = "accountService")
    public BalanceResponse debit(UUID accountId, BigDecimal amount, String transactionId) {
        return updateBalance(accountId, amount, BalanceOperation.DEBIT, transactionId);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "accountServiceFallback")
    @Retry(name = "accountService")
    public BalanceResponse credit(UUID accountId, BigDecimal amount, String transactionId) {
        return updateBalance(accountId, amount, BalanceOperation.CREDIT, transactionId);
    }

    private BalanceResponse updateBalance(UUID accountId, BigDecimal amount, BalanceOperation operation, String transactionId) {
        UpdateBalanceRequest requestBody = new UpdateBalanceRequest(amount, operation, transactionId);
        try {
            return accountServiceRestClient.patch()
                    .uri("/api/v1/accounts/{accountId}/balance", accountId)
                    .header("X-Transaction-Id", transactionId)
                    .body(requestBody)
                    .retrieve()
                    .body(BalanceResponse.class);
        } catch (HttpClientErrorException ex) {
            throw mapClientError(accountId, ex);
        }
    }

    /**
     * Maps a 4xx response from account-service into the corresponding
     * common exception. account-service's own OpenAPI spec returns 409 for
     * both "account inactive" and "insufficient balance" cases, so the
     * response body's errorCode field is inspected to disambiguate; if the
     * body is missing/unparseable, AccountInactiveException is used as the
     * safe default for 409.
     */
    private BusinessException mapClientError(UUID accountId, HttpClientErrorException ex) {
        HttpStatusCode status = ex.getStatusCode();
        if (status.value() == 404) {
            return new AccountNotFoundException(accountId.toString());
        }
        if (status.value() == 422) {
            return new InsufficientBalanceException(accountId.toString());
        }
        if (status.value() == 409) {
            String errorCode = extractErrorCode(ex);
            if (ErrorCode.TXN_INSUFFICIENT_BALANCE.name().equals(errorCode)) {
                return new InsufficientBalanceException(accountId.toString());
            }
            return new AccountInactiveException(accountId.toString());
        }
        log.warn("Unmapped 4xx response from account-service for account {}: {}", accountId, status);
        return new AccountInactiveException(accountId.toString());
    }

    private String extractErrorCode(HttpClientErrorException ex) {
        try {
            AccountServiceErrorBody body = ex.getResponseBodyAs(AccountServiceErrorBody.class);
            return body != null ? body.errorCode() : null;
        } catch (Exception parseEx) {
            log.debug("Could not parse account-service error body: {}", parseEx.getMessage());
            return null;
        }
    }

    /**
     * Shared Resilience4j fallback for both {@link #debit} and
     * {@link #credit} (identical signatures: accountId, amount,
     * transactionId, Throwable). Invoked when the circuit is OPEN or
     * retries are exhausted for transport-level failures. Business
     * exceptions (4xx mapped in {@link #mapClientError}) are excluded from
     * retry via ignoreExceptions in application.yml, but a fallback is still
     * invoked by the circuit breaker aspect - in that case we simply
     * re-throw the original business exception unchanged.
     */
    @SuppressWarnings("unused")
    private BalanceResponse accountServiceFallback(UUID accountId, BigDecimal amount, String transactionId, Throwable throwable) {
        if (throwable instanceof BusinessException businessException) {
            throw businessException;
        }
        log.error("account-service unavailable for account {} (transactionId={}): {}",
                accountId, transactionId, throwable.getMessage());
        throw new BusinessException(ErrorCode.ACCOUNT_SERVICE_UNAVAILABLE);
    }
}
