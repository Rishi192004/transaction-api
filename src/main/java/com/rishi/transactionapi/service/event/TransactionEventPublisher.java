package com.rishi.transactionapi.service.event;

import com.rishi.transactionapi.model.Transaction;
import com.rishi.transactionapi.model.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for publishing transaction events to Kafka
 * Handles async settlement and event-driven architecture
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";
    private static final String SETTLEMENT_EVENTS_TOPIC = "settlement-events";

    /**
     * Publish transaction created event
     */
    public void publishTransactionCreated(Transaction transaction) {
        TransactionEvent event = TransactionEvent.fromTransaction(transaction, "CREATED");
        publishEvent(event, TRANSACTION_EVENTS_TOPIC);
        log.info("Published CREATED event for transaction: {}", transaction.getId());
    }

    /**
     * Publish transaction updated event
     */
    public void publishTransactionUpdated(Transaction transaction) {
        TransactionEvent event = TransactionEvent.fromTransaction(transaction, "UPDATED");
        publishEvent(event, TRANSACTION_EVENTS_TOPIC);
        log.info("Published UPDATED event for transaction: {}", transaction.getId());
    }

    /**
     * Publish settlement event for async processing
     */
    public void publishSettlementEvent(Transaction transaction) {
        TransactionEvent event = TransactionEvent.createSettlementEvent(transaction);
        publishEvent(event, SETTLEMENT_EVENTS_TOPIC);
        log.info("Published SETTLEMENT event for transaction: {} - Account: {}", transaction.getId(), transaction.getAccountId());
    }

    /**
     * Generic method to publish event to Kafka
     */
    private void publishEvent(TransactionEvent event, String topic) {
        try {
            String messageKey = event.getAccountId(); // Partition by account ID for ordering
            String messageId = UUID.randomUUID().toString();

            Message<TransactionEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(KafkaHeaders.MESSAGE_KEY, messageKey)
                    .setHeader("X-Message-ID", messageId)
                    .setHeader("X-Event-Type", event.getEventType())
                    .setHeader("X-Transaction-ID", event.getTransactionId())
                    .build();

            kafkaTemplate.send(message).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Event published successfully. Topic: {}, MessageID: {}, Offset: {}",
                            topic, messageId, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event to topic: {} - Error: {}", topic, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing event to topic {}: {}", topic, e.getMessage(), e);
        }
    }

    /**
     * Batch publish events (for bulk operations)
     */
    public void publishBatchEvents(java.util.List<TransactionEvent> events, String topic) {
        events.forEach(event -> publishEvent(event, topic));
        log.info("Batch published {} events to topic: {}", events.size(), topic);
    }
}
