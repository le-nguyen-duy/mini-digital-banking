package com.bankingdemo.accountservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks transactionId values already applied to an account's balance so
 * that retried/duplicated balance-update requests (identified by the
 * X-Transaction-Id header) are idempotent, per requirement doc section 2.4.
 */
@Entity
@Table(name = "processed_transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "transaction_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}
