package com.bankingdemo.transactionservice.repository;

import com.bankingdemo.transactionservice.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    /**
     * Used to enforce the Idempotency-Key contract from the OpenAPI spec:
     * a repeated key returns TXN_DUPLICATE_REQUEST (409) instead of creating
     * a second transaction.
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
