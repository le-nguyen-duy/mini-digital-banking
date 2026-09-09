package com.bankingdemo.transactionservice.service;

import com.bankingdemo.common.exception.BusinessException;
import com.bankingdemo.common.exception.DuplicateTransactionException;
import com.bankingdemo.common.exception.ErrorCode;
import com.bankingdemo.common.exception.InvalidAmountException;
import com.bankingdemo.common.exception.TransactionNotFoundException;
import com.bankingdemo.common.kafka.TransactionCompletedEvent;
import com.bankingdemo.common.kafka.TransactionCreatedEvent;
import com.bankingdemo.common.kafka.TransactionFailedEvent;
import com.bankingdemo.transactionservice.client.AccountServiceClient;
import com.bankingdemo.transactionservice.domain.Transaction;
import com.bankingdemo.transactionservice.domain.TransactionStatus;
import com.bankingdemo.transactionservice.dto.CreateTransactionRequest;
import com.bankingdemo.transactionservice.dto.TransactionPageResponse;
import com.bankingdemo.transactionservice.dto.TransactionResponse;
import com.bankingdemo.transactionservice.kafka.TransactionEventPublisher;
import com.bankingdemo.transactionservice.repository.TransactionRepository;
import com.bankingdemo.transactionservice.repository.TransactionSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionEventPublisher transactionEventPublisher;

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request, String idempotencyKey, String customerId) {
        // Defensive check: bean validation on CreateTransactionRequest already
        // enforces amount >= 0.01, this is a belt-and-suspenders business rule.
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Số tiền giao dịch phải lớn hơn 0");
        }

        transactionRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(existing -> {
                    throw new DuplicateTransactionException(idempotencyKey);
                });

        Transaction transaction = new Transaction();
        transaction.setFromAccount(request.fromAccount());
        transaction.setToAccount(request.toAccount());
        transaction.setAmount(request.amount());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCustomerId(customerId);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction = transactionRepository.save(transaction);

        String transactionId = transaction.getId().toString();
        transactionEventPublisher.publishCreated(new TransactionCreatedEvent(
                transactionId,
                request.fromAccount().toString(),
                request.toAccount().toString(),
                request.amount()
        ));

        try {
            accountServiceClient.debit(request.fromAccount(), request.amount(), transactionId);
        } catch (BusinessException ex) {
            failTransaction(transaction, ex.getErrorCode().name(), customerId, transactionId);
            throw ex;
        }

        try {
            accountServiceClient.credit(request.toAccount(), request.amount(), transactionId);
        } catch (Exception ex) {
            // TODO (requirement doc section 4 "Consistency" row): in a real
            // system this would trigger a compensating action - a reversal
            // credit back to fromAccount to undo the debit that already
            // succeeded. Out of scope for this demo; the transaction is
            // simply marked FAILED and left for manual reconciliation.
            log.error("Credit to account {} failed after successful debit for transactionId {}: {}",
                    request.toAccount(), transactionId, ex.getMessage(), ex);
            failTransaction(transaction, ErrorCode.ACCOUNT_SERVICE_UNAVAILABLE.name(), customerId, transactionId);
            throw ex;
        }

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);
        transactionEventPublisher.publishCompleted(new TransactionCompletedEvent(
                transactionId, transaction.getStatus().name(), customerId
        ));

        return TransactionResponse.from(transaction);
    }

    private void failTransaction(Transaction transaction, String errorCode, String customerId, String transactionId) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setErrorCode(errorCode);
        transactionRepository.save(transaction);
        transactionEventPublisher.publishFailed(new TransactionFailedEvent(
                transactionId, errorCode, customerId
        ));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.toString()));
        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactionHistory(UUID accountId, int page, int size, TransactionStatus status) {
        var spec = TransactionSpecifications.withFilters(accountId, status);
        var result = transactionRepository.findAll(spec, PageRequest.of(page, size));
        return TransactionPageResponse.from(result);
    }
}
