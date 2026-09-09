package com.bankingdemo.transactionservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * transactions (id, from_account, to_account, amount, status, error_code,
 * created_at, updated_at) — see requirement doc section 11. Also carries
 * {@code customerId} (owner of fromAccount, needed for notification events)
 * and {@code idempotencyKey} (backing the Idempotency-Key header requirement
 * from the OpenAPI spec / 409 TXN_DUPLICATE_REQUEST).
 */
@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"idempotency_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "from_account", nullable = false)
    private UUID fromAccount;

    @Column(name = "to_account", nullable = false)
    private UUID toAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
