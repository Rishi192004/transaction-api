package com.rishi.transactionapi.exception;

/**
 * Exception thrown when an account exceeds its rate limit
 */
public class RateLimitExceededException extends RuntimeException {
    private final String accountId;

    public RateLimitExceededException(String accountId) {
        super(String.format("Rate limit exceeded for account: %s. Maximum requests per minute limit reached.", accountId));
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
