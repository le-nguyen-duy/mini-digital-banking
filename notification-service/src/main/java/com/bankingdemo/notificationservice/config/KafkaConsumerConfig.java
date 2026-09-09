package com.bankingdemo.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

/**
 * Consumer-side Kafka configuration. Uses {@link ErrorHandlingDeserializer}
 * wrapping a {@link JsonDeserializer} so that poison-pill messages (bad JSON,
 * unknown type) don't crash the listener container.
 *
 * A {@link DefaultErrorHandler} with a fixed backoff is registered so that
 * transient processing failures are retried a bounded number of times before
 * being skipped. Requirement doc section 8.2 calls for a Dead Letter Topic
 * (DLT) for messages that keep failing; wiring a
 * {@code DeadLetterPublishingRecoverer} here (backed by the same
 * KafkaTemplate producer config used elsewhere) would be the natural next
 * step, but is left as a future improvement for this demo — currently the
 * error handler logs and skips the record after retries are exhausted.
 */
@EnableKafka
@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES, "com.bankingdemo.common.kafka"
        );
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    /**
     * Retries a failing record up to 2 extra times (200ms apart), then logs
     * and skips it so the consumer keeps making progress. See class javadoc
     * regarding the DLT follow-up.
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(200L, 2L));
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Error processing Kafka record (topic={}, attempt={}): {}",
                        record.topic(), deliveryAttempt, ex.getMessage()));
        return handler;
    }
}
