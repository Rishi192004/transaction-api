package com.rishi.transactionapi.service.event;

import com.rishi.transactionapi.model.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Listener for settlement events
 * Processes async settlement for transactions
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class SettlementEventListener {

    /**
     * Listen to settlement events and process them asynchronously
     */
    @KafkaListener(
            topics = "settlement-events",
            groupId = "transaction-settlement-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processSettlementEvent(
            @Payload TransactionEvent event,
            @Header(name = "X-Message-ID", required = false) String messageId,
            @Header(name = "kafka_receivedPartition", required = false) int partition,
            @Header(name = "kafka_receivedTopic", required = false) String topic) {

        try {
            log.info("Processing settlement event. MessageID: {}, TransactionID: {}, AccountID: {}, Amount: {}, Topic: {}, Partition: {}",
                    messageId, event.getTransactionId(), event.getAccountId(), event.getAmount(), topic, partition);

            // Simulate settlement processing
            processSettlement(event);

            log.info("Settlement completed successfully for transaction: {}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing settlement event for transaction: {} - Error: {}", 
                    event.getTransactionId(), e.getMessage(), e);
            // In production, implement dead-letter queue or retry mechanism
        }
    }

    /**
     * Listen to transaction created events
     */
    @KafkaListener(
            topics = "transaction-events",
            groupId = "transaction-processing-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processTransactionEvent(
            @Payload TransactionEvent event,
            @Header(name = "X-Message-ID", required = false) String messageId) {

        try {
            log.info("Processing transaction event. MessageID: {}, EventType: {}, TransactionID: {}, Status: {}",
                    messageId, event.getEventType(), event.getTransactionId(), event.getStatus());

            switch (event.getEventType()) {
                case "CREATED":
                    handleTransactionCreated(event);
                    break;
                case "UPDATED":
                    handleTransactionUpdated(event);
                    break;
                case "SETTLED":
                    handleTransactionSettled(event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing transaction event: {} - Error: {}", 
                    event.getTransactionId(), e.getMessage(), e);
        }
    }

    private void processSettlement(TransactionEvent event) {
        // Simulate settlement processing with external payment processor
        log.info("Settling transaction: {} for account: {} - Amount: {}", 
                event.getTransactionId(), event.getAccountId(), event.getAmount());
        
        // Step 1: Verify transaction
        verifyTransaction(event);
        
        // Step 2: Process with payment gateway
        processWithPaymentGateway(event);
        
        // Step 3: Update settlement status
        updateSettlementStatus(event);
    }

    private void verifyTransaction(TransactionEvent event) {
        log.debug("Verifying transaction: {} - Idempotency Key: {}", 
                event.getTransactionId(), event.getIdempotencyKey());
        // Verification logic
    }

    private void processWithPaymentGateway(TransactionEvent event) {
        log.debug("Processing transaction {} with payment gateway - Type: {}, Amount: {}", 
                event.getTransactionId(), event.getType(), event.getAmount());
        
        // Simulate API call to payment processor
        try {
            Thread.sleep(100); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted: {}", e.getMessage());
        }
    }

    private void updateSettlementStatus(TransactionEvent event) {
        log.debug("Updating settlement status for transaction: {} to COMPLETED", event.getTransactionId());
        // Update transaction status in database
    }

    private void handleTransactionCreated(TransactionEvent event) {
        log.debug("Handling transaction created event: {}", event.getTransactionId());
        // Additional processing if needed
    }

    private void handleTransactionUpdated(TransactionEvent event) {
        log.debug("Handling transaction updated event: {}", event.getTransactionId());
        // Additional processing if needed
    }

    private void handleTransactionSettled(TransactionEvent event) {
        log.debug("Handling transaction settled event: {}", event.getTransactionId());
        // Mark settlement as complete in database
    }
}
