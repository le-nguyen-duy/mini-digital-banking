package com.bankingdemo.transactionservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

/**
 * Serializes a domain event and appends it to the outbox_events table.
 * Must always be called from inside the same {@code @Transactional} method
 * that performs the business write (e.g. TransactionService#createTransaction),
 * so the transaction state change and the outbox row are committed
 * atomically.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void append(String aggregateType, String aggregateId, String topic, Object event) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setEventType(event.getClass().getName());
        outboxEvent.setTopic(topic);
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setRetryCount(0);
        outboxEventRepository.save(outboxEvent);
    }
}
