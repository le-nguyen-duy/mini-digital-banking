package com.bankingdemo.transactionservice.client;

/**
 * Minimal shape used to parse the {@code errorCode} field out of an
 * account-service {@code ErrorResponse} body (com.bankingdemo.common.dto.ErrorResponse)
 * when mapping 4xx responses into specific business exceptions. Only the
 * field we need is declared; Jackson ignores the rest.
 */
public record AccountServiceErrorBody(String errorCode) {
}
