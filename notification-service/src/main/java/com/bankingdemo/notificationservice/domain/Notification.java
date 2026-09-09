package com.bankingdemo.notificationservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * notifications (id, transaction_id, customer_id, channel, status, message, sent_at)
 * See requirement doc section 11.
 *
 * A unique constraint on transaction_id enforces idempotency: each
 * transaction produces exactly one completed OR failed event, so at most
 * one notification row should ever exist per transactionId. This backs the
 * idempotent-consumer check performed by NotificationService before insert.
 */
@Entity
@Table(name = "notifications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notifications_transaction_id", columnNames = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }
}
