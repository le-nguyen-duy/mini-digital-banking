package com.bankingdemo.accountservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls outbox_events for PENDING rows and relays them to Kafka.
 *
 * This is the piece that makes publishing resilient to a broker outage: if
 * Kafka is unreachable when the business transaction commits, the event is
 * already durably stored in Postgres (status PENDING) as part of that same
 * transaction. This poller simply retries on every run (fixedDelay) until
 * Kafka is back, instead of the event being silently lost like the old
 * fire-and-forget KafkaTemplate.send() call used to do.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRY_BEFORE_FAILED = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:2000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outboxEventRepository.findBatchForRelay(BATCH_SIZE);
        for (OutboxEvent outboxEvent : batch) {
            relayOne(outboxEvent);
        }
    }

    private void relayOne(OutboxEvent outboxEvent) {
        try {
            // Publish synchronously (.get()) inside the poll loop: we need to
            // know the outcome before deciding whether to mark this row SENT
            // or leave it PENDING for the next poll. The row-level DB lock
            // (FOR UPDATE SKIP LOCKED) held for the duration of this
            // transaction is what keeps a second instance from re-sending
            // the same row concurrently.
            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getAggregateId(), deserializePayload(outboxEvent))
                    .get();
            outboxEvent.setStatus(OutboxStatus.SENT);
            outboxEvent.setSentAt(Instant.now());
            log.info("Relayed outbox event {} (topic={}, aggregateId={})",
                    outboxEvent.getId(), outboxEvent.getTopic(), outboxEvent.getAggregateId());
        } catch (Exception ex) {
            outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
            outboxEvent.setLastError(ex.getMessage());
            if (outboxEvent.getRetryCount() >= MAX_RETRY_BEFORE_FAILED) {
                outboxEvent.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event {} exceeded max retries ({}), marking FAILED for manual reconciliation: {}",
                        outboxEvent.getId(), MAX_RETRY_BEFORE_FAILED, ex.getMessage(), ex);
            } else {
                log.warn("Failed to relay outbox event {} (attempt {}): {}",
                        outboxEvent.getId(), outboxEvent.getRetryCount(), ex.getMessage());
            }
        }
        outboxEventRepository.save(outboxEvent);
    }

    private Object deserializePayload(OutboxEvent outboxEvent) {
        try {
            Class<?> eventClass = Class.forName(outboxEvent.getEventType());
            return objectMapper.readValue(outboxEvent.getPayload(), eventClass);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize outbox payload for event " + outboxEvent.getId(), ex);
        }
    }
}
