package com.rishi.transactionapi.config.redis;

import com.rishi.transactionapi.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching account-related data in Redis
 * Caches: account balances, recent transactions, and rate limit info
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "true", matchIfMissing = true)
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Cache TTL in minutes
    private static final long BALANCE_CACHE_TTL = 5;
    private static final long TRANSACTION_CACHE_TTL = 10;
    private static final long ACCOUNT_CACHE_TTL = 30;

    // Cache key prefixes
    private static final String BALANCE_PREFIX = "account:balance:";
    private static final String RECENT_TXN_PREFIX = "account:recent_txn:";
    private static final String ACCOUNT_PREFIX = "account:info:";

    /**
     * Cache account balance
     */
    public void cacheAccountBalance(String accountId, BigDecimal balance) {
        String cacheKey = BALANCE_PREFIX + accountId;
        try {
            redisTemplate.opsForValue().set(cacheKey, balance, BALANCE_CACHE_TTL, TimeUnit.MINUTES);
            log.debug("Cached account balance for {}: {}", accountId, balance);
        } catch (Exception e) {
            log.error("Error caching account balance for {}: {}", accountId, e.getMessage());
        }
    }

    /**
     * Get cached account balance
     */
    public Optional<BigDecimal> getAccountBalance(String accountId) {
        String cacheKey = BALANCE_PREFIX + accountId;
        try {
            Object cachedBalance = redisTemplate.opsForValue().get(cacheKey);
            if (cachedBalance != null) {
                log.debug("Retrieved cached balance for account: {}", accountId);
                return Optional.of((BigDecimal) cachedBalance);
            }
        } catch (Exception e) {
            log.error("Error retrieving account balance from cache for {}: {}", accountId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Cache recent transactions for an account
     */
    public void cacheRecentTransactions(String accountId, List<TransactionDTO.Response> transactions) {
        String cacheKey = RECENT_TXN_PREFIX + accountId;
        try {
            redisTemplate.delete(cacheKey);
            if (!transactions.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(cacheKey, transactions.toArray());
                redisTemplate.expire(cacheKey, TRANSACTION_CACHE_TTL, TimeUnit.MINUTES);
                log.debug("Cached {} recent transactions for account: {}", transactions.size(), accountId);
            }
        } catch (Exception e) {
            log.error("Error caching recent transactions for {}: {}", accountId, e.getMessage());
        }
    }

    /**
     * Get cached recent transactions
     */
    @SuppressWarnings("unchecked")
    public Optional<List<TransactionDTO.Response>> getRecentTransactions(String accountId) {
        String cacheKey = RECENT_TXN_PREFIX + accountId;
        try {
            List<Object> cachedTransactions = redisTemplate.opsForList().range(cacheKey, 0, -1);
            if (cachedTransactions != null && !cachedTransactions.isEmpty()) {
                log.debug("Retrieved {} cached transactions for account: {}", cachedTransactions.size(), accountId);
                return Optional.of(cachedTransactions.stream()
                        .map(obj -> (TransactionDTO.Response) obj)
                        .toList());
            }
        } catch (Exception e) {
            log.error("Error retrieving cached transactions for {}: {}", accountId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Invalidate balance cache for an account
     */
    public void invalidateBalance(String accountId) {
        String cacheKey = BALANCE_PREFIX + accountId;
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (deleted != null && deleted) {
                log.debug("Invalidated balance cache for account: {}", accountId);
            }
        } catch (Exception e) {
            log.error("Error invalidating balance cache for {}: {}", accountId, e.getMessage());
        }
    }

    /**
     * Invalidate recent transactions cache for an account
     */
    public void invalidateRecentTransactions(String accountId) {
        String cacheKey = RECENT_TXN_PREFIX + accountId;
        try {
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (deleted != null && deleted) {
                log.debug("Invalidated recent transactions cache for account: {}", accountId);
            }
        } catch (Exception e) {
            log.error("Error invalidating transactions cache for {}: {}", accountId, e.getMessage());
        }
    }

    /**
     * Invalidate all caches for an account
     */
    public void invalidateAccountCache(String accountId) {
        invalidateBalance(accountId);
        invalidateRecentTransactions(accountId);
        
        String accountInfoKey = ACCOUNT_PREFIX + accountId;
        try {
            redisTemplate.delete(accountInfoKey);
            log.info("Invalidated all caches for account: {}", accountId);
        } catch (Exception e) {
            log.error("Error invalidating account cache for {}: {}", accountId, e.getMessage());
        }
    }

    /**
     * Clear all caches (admin function)
     */
    public void clearAllCaches() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            log.info("All Redis caches cleared");
        } catch (Exception e) {
            log.error("Error clearing all caches: {}", e.getMessage());
        }
    }
}
