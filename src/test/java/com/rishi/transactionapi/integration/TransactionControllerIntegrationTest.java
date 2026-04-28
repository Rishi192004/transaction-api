package com.rishi.transactionapi.integration;

import com.rishi.transactionapi.config.ratelimiter.AccountRateLimitStore;
import com.rishi.transactionapi.dto.TransactionDTO;
import com.rishi.transactionapi.model.Transaction;
import com.rishi.transactionapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Transaction API Integration Tests")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRateLimitStore rateLimitStore;

    private TransactionDTO.Request transactionRequest;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        rateLimitStore.resetBucket("testuser");

        transactionRequest = TransactionDTO.Request.builder()
                .accountId("ACC001")
                .amount(BigDecimal.valueOf(500.00))
                .type(Transaction.TransactionType.DEPOSIT)
                .description("Integration test deposit")
                .idempotencyKey("IDEM-001")
                .build();
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /api/v1/transactions - Create transaction successfully")
    void testCreateTransactionSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.accountId").value("ACC001"))
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("POST /api/v1/transactions - Reject duplicate idempotency key")
    void testCreateTransactionDuplicate() throws Exception {
        // First request succeeds
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated());

        // Second request with same idempotency key should fail
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("GET /api/v1/transactions/{id} - Get transaction by ID")
    void testGetTransactionSuccess() throws Exception {
        // Create transaction first
        TransactionDTO.Request req = TransactionDTO.Request.builder()
                .accountId("ACC002")
                .amount(BigDecimal.valueOf(1000))
                .type(Transaction.TransactionType.WITHDRAWAL)
                .description("Test withdrawal")
                .idempotencyKey("IDEM-002")
                .build();

        var createResponse = mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long transactionId = objectMapper.readValue(createResponse, TransactionDTO.Response.class).getId();

        // Fetch transaction
        mockMvc.perform(get("/api/v1/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.accountId").value("ACC002"))
                .andExpect(jsonPath("$.amount").value(1000.0));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("GET /api/v1/transactions/account/{accountId} - Get transactions by account with caching")
    void testGetTransactionsByAccountCached() throws Exception {
        // Create multiple transactions
        for (int i = 0; i < 3; i++) {
            var req = TransactionDTO.Request.builder()
                    .accountId("ACC003")
                    .amount(BigDecimal.valueOf(100 * (i + 1)))
                    .type(Transaction.TransactionType.DEPOSIT)
                    .description("Deposit " + i)
                    .idempotencyKey("IDEM-003-" + i)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // First request (cache miss)
        mockMvc.perform(get("/api/v1/transactions/account/ACC003")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));

        // Second request (should be cached)
        mockMvc.perform(get("/api/v1/transactions/account/ACC003")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(3)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Rate limiting - exceed requests per minute")
    void testRateLimitingExceeded() throws Exception {
        // Create bucket for user and set low limit
        rateLimitStore.setCustomLimit("testuser", 2); // 2 requests per minute

        // First two requests should succeed
        for (int i = 0; i < 2; i++) {
            var req = transactionRequest;
            req.setIdempotencyKey("IDEM-LIMIT-" + i);
            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // Third request should be rate limited
        var req = transactionRequest;
        req.setIdempotencyKey("IDEM-LIMIT-3");
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("PATCH /api/v1/transactions/{id}/reverse - Reverse completed transaction")
    void testReverseTransactionSuccess() throws Exception {
        // Create transaction
        var createResponse = mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long transactionId = objectMapper.readValue(createResponse, TransactionDTO.Response.class).getId();

        // Reverse transaction
        mockMvc.perform(patch("/api/v1/transactions/{id}/reverse", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("Reverse transaction - admin only (unauthorized)")
    void testReverseTransactionUnauthorized() throws Exception {
        // Create transaction
        var createResponse = mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long transactionId = objectMapper.readValue(createResponse, TransactionDTO.Response.class).getId();

        // Try to reverse without ADMIN role
        mockMvc.perform(patch("/api/v1/transactions/{id}/reverse", transactionId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    @DisplayName("Validation - missing required fields")
    void testCreateTransactionValidationError() throws Exception {
        var invalidRequest = TransactionDTO.Request.builder()
                .accountId("") // Invalid: empty account
                .amount(BigDecimal.ZERO) // Invalid: zero amount
                .type(null)
                .description("Test")
                .build();

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthorized access without authentication")
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/1"))
                .andExpect(status().isUnauthorized());
    }
}
