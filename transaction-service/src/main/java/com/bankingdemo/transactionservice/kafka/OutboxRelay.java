package com.bankingdemo.transactionservice.kafka;

import com.bankingdemo.transactionservice.outbox.OutboxEvent;
import com.bankingdemo.transactionservice.outbox.OutboxEventRepository;
import com.bankingdemo.transactionservice.outbox.OutboxStatus;
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
 * Polls outbox_events for PENDING rows and relays them to Kafka. If Kafka
 * is unreachable when a transaction commits, the event is already durably
 * stored in Postgres (status PENDING) as part of that same DB transaction -
 * this poller retries on every run until Kafka is back, instead of the
 * event being silently lost like the old fire-and-forget
 * KafkaTemplate.send() call used to do.
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
