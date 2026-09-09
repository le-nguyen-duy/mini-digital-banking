package com.bankingdemo.accountservice.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * outbox_events (id, aggregate_type, aggregate_id, event_type, topic,
 * payload, status, created_at, sent_at, retry_count, last_error).
 *
 * Written by *EventPublisher classes in the same DB transaction as the
 * business write (e.g. AccountService#updateBalance), so the balance change
 * and the "intent to publish" either both commit or both roll back.
 * {@link com.bankingdemo.accountservice.kafka.OutboxRelay} asynchronously
 * relays PENDING rows to Kafka afterwards. See requirement doc section 4
 * "Consistency" - solves the dual-write problem between Postgres and Kafka.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /**
     * Fully-qualified class name of the event record (e.g.
     * {@code com.bankingdemo.common.kafka.AccountBalanceUpdatedEvent}), so
     * the relay can deserialize {@link #payload} back into the correct type
     * before handing it to KafkaTemplate - this keeps the on-wire format
     * (JSON + Spring Kafka's __TypeId__ header) identical to what consumers
     * already expect, no consumer-side changes needed.
     */
    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
    }
}
