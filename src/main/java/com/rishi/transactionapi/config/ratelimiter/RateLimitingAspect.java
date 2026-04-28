package com.rishi.transactionapi.config.ratelimiter;

import com.rishi.transactionapi.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for applying rate limiting to transaction endpoints
 * Extracts account ID from security context and applies per-account limits
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitingAspect {

    private final AccountRateLimitStore rateLimitStore;

    @Around("@annotation(com.rishi.transactionapi.config.ratelimiter.RateLimited)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        String accountId = extractAccountIdFromContext();
        
        if (accountId == null) {
            log.warn("Could not extract account ID from security context");
            return joinPoint.proceed();
        }

        if (!rateLimitStore.allowRequest(accountId)) {
            log.warn("Rate limit exceeded for account: {}. Method: {}", accountId, joinPoint.getSignature());
            throw new RateLimitExceededException(accountId);
        }

        return joinPoint.proceed();
    }

    /**
     * Extract account ID from security context
     * Can be customized based on your security implementation
     */
    private String extractAccountIdFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // Using the username/principal as account ID
            // In a real system, this might be extracted from a JWT claim or user details
            return authentication.getName();
        }
        return null;
    }
}
