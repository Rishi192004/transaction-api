package com.rishi.transactionapi.dto;

import com.rishi.transactionapi.model.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Account ID is required")
        private String accountId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        @NotNull(message = "Transaction type is required")
        private Transaction.TransactionType type;

        @NotBlank(message = "Description is required")
        private String description;

        // Optional: client-supplied idempotency key
        private String idempotencyKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String accountId;
        private BigDecimal amount;
        private Transaction.TransactionType type;
        private String description;
        private Transaction.TransactionStatus status;
        private LocalDateTime createdAt;
        private String idempotencyKey;

        public static Response from(Transaction t) {
            return Response.builder()
                    .id(t.getId())
                    .accountId(t.getAccountId())
                    .amount(t.getAmount())
                    .type(t.getType())
                    .description(t.getDescription())
                    .status(t.getStatus())
                    .createdAt(t.getCreatedAt())
                    .idempotencyKey(t.getIdempotencyKey())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResponse {
        private java.util.List<Response> transactions;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
