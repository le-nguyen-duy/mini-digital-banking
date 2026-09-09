package com.bankingdemo.transactionservice.repository;

import com.bankingdemo.transactionservice.domain.Transaction;
import com.bankingdemo.transactionservice.domain.TransactionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Builds a composable {@link Specification} for the transaction history
 * lookup exposed by GET /api/v1/transactions: matches transactions where the
 * given accountId is either the fromAccount or the toAccount, optionally
 * narrowed down by status.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> withFilters(UUID accountId, TransactionStatus status) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("fromAccount"), accountId),
                    criteriaBuilder.equal(root.get("toAccount"), accountId)
            );
            if (status != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("status"), status));
            }
            return predicate;
        };
    }
}
