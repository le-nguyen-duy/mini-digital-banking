package com.bankingdemo.accountservice.repository;

import com.bankingdemo.accountservice.domain.ProcessedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedTransactionRepository extends JpaRepository<ProcessedTransaction, Long> {

    Optional<ProcessedTransaction> findByAccountIdAndTransactionId(UUID accountId, String transactionId);
}
