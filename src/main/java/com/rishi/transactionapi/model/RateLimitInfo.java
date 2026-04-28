package com.rishi.transactionapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks rate limit information for an account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitInfo {
    private String accountId;
    private long requestsAllowed;
    private long requestsUsed;
    private long refillRatePerMinute;
    private LocalDateTime resetTime;
    private boolean isLimited;

    public boolean canProceedRequest() {
        return !isLimited && requestsUsed < requestsAllowed;
    }

    public void consumeToken() {
        this.requestsUsed++;
    }

    public long getRemainingTokens() {
        return Math.max(0, requestsAllowed - requestsUsed);
    }
}
