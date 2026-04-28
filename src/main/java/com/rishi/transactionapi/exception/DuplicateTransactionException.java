package com.rishi.transactionapi.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String idempotencyKey, Long existingId) {
        super("Duplicate transaction detected. Idempotency key: " + idempotencyKey
                + " already maps to transaction id: " + existingId);
    }
}
