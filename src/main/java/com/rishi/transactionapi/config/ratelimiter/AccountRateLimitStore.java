package com.rishi.transactionapi.config.ratelimiter;

import com.rishi.transactionapi.model.RateLimitInfo;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and manages rate limit buckets per account
 * Using Token Bucket Algorithm with per-account limits
 */
@Slf4j
@Component
public class AccountRateLimitStore {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, RateLimitInfo> rateLimitInfos = new ConcurrentHashMap<>();

    private static final long DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final long DEFAULT_REQUESTS_PER_HOUR = 5000;

    /**
     * Get or create a bucket for an account
     */
    public Bucket getOrCreateBucket(String accountId) {
        return buckets.computeIfAbsent(accountId, key -> createNewBucket());
    }

    /**
     * Create a new bucket with default limits
     * 100 requests per minute, 5000 requests per hour
     */
    private Bucket createNewBucket() {
        Bandwidth minuteLimit = Bandwidth.classic(DEFAULT_REQUESTS_PER_MINUTE, Refill.intervally(DEFAULT_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        Bandwidth hourLimit = Bandwidth.classic(DEFAULT_REQUESTS_PER_HOUR, Refill.intervally(DEFAULT_REQUESTS_PER_HOUR, Duration.ofHours(1)));
        return Bucket4j.builder()
                .addLimit(minuteLimit)
                .addLimit(hourLimit)
                .build();
    }

    /**
     * Check if account can proceed with request
     */
    public boolean allowRequest(String accountId) {
        Bucket bucket = getOrCreateBucket(accountId);
        boolean allowed = bucket.tryConsume(1);
        
        if (allowed) {
            log.debug("Request allowed for account: {}. Remaining tokens: {}", accountId, bucket.estimateAbilityToConsume(1));
        } else {
            log.warn("Rate limit exceeded for account: {}", accountId);
        }
        
        return allowed;
    }

    /**
     * Get current rate limit info for an account
     */
    public RateLimitInfo getRateLimitInfo(String accountId) {
        Bucket bucket = getOrCreateBucket(accountId);
        
        return rateLimitInfos.computeIfAbsent(accountId, key -> RateLimitInfo.builder()
                .accountId(accountId)
                .requestsAllowed(DEFAULT_REQUESTS_PER_MINUTE)
                .requestsUsed(0)
                .refillRatePerMinute(DEFAULT_REQUESTS_PER_MINUTE)
                .resetTime(LocalDateTime.now().plusMinutes(1))
                .isLimited(false)
                .build());
    }

    /**
     * Reset bucket for an account (admin function)
     */
    public void resetBucket(String accountId) {
        buckets.remove(accountId);
        rateLimitInfos.remove(accountId);
        log.info("Rate limit bucket reset for account: {}", accountId);
    }

    /**
     * Set custom limit for an account (admin function)
     */
    public void setCustomLimit(String accountId, long requestsPerMinute) {
        buckets.remove(accountId);
        rateLimitInfos.remove(accountId);
        
        Bandwidth customLimit = Bandwidth.classic(requestsPerMinute, Refill.intervally(requestsPerMinute, Duration.ofMinutes(1)));
        Bucket customBucket = Bucket4j.builder()
                .addLimit(customLimit)
                .build();
        
        buckets.put(accountId, customBucket);
        log.info("Custom rate limit set for account: {} - {} requests/minute", accountId, requestsPerMinute);
    }
}
