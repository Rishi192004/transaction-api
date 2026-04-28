package com.rishi.transactionapi.model.event;

import com.rishi.transactionapi.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event model for transaction events published to Kafka
 * Used for async settlement processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long transactionId;
    private String accountId;
    private String type; // DEPOSIT, WITHDRAWAL
    private BigDecimal amount;
    private String description;
    private String status; // PENDING, COMPLETED, FAILED
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime eventPublishedAt;
    private String eventType; // CREATED, UPDATED, SETTLED
    private String settlementStatus; // PENDING, PROCESSING, COMPLETED, FAILED

    /**
     * Create event from transaction
     */
    public static TransactionEvent fromTransaction(Transaction transaction, String eventType) {
        return TransactionEvent.builder()
                .transactionId(transaction.getId())
                .accountId(transaction.getAccountId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .idempotencyKey(transaction.getIdempotencyKey())
                .createdAt(transaction.getCreatedAt())
                .eventPublishedAt(LocalDateTime.now())
                .eventType(eventType)
                .settlementStatus("PENDING")
                .build();
    }

    /**
     * Create settled event
     */
    public static TransactionEvent createSettlementEvent(Transaction transaction) {
        return TransactionEvent.builder()
                .transactionId(transaction.getId())
                .accountId(transaction.getAccountId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .idempotencyKey(transaction.getIdempotencyKey())
                .createdAt(transaction.getCreatedAt())
                .eventPublishedAt(LocalDateTime.now())
                .eventType("SETTLED")
                .settlementStatus("COMPLETED")
                .build();
    }
}
