package com.rishi.transactionapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishi.transactionapi.dto.TransactionDTO;
import com.rishi.transactionapi.model.Transaction;
import com.rishi.transactionapi.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(com.rishi.transactionapi.config.SecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private TransactionDTO.Response mockResponse() {
        return TransactionDTO.Response.builder()
                .id(1L)
                .accountId("ACC-001")
                .amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.CREDIT)
                .description("Salary credit")
                .status(Transaction.TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .idempotencyKey("key-abc")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/transactions - should return 201 for valid request")
    @WithMockUser(roles = "USER")
    void createTransaction_returns201() throws Exception {
        TransactionDTO.Request request = TransactionDTO.Request.builder()
                .accountId("ACC-001")
                .amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.CREDIT)
                .description("Salary credit")
                .build();

        when(transactionService.createTransaction(any())).thenReturn(mockResponse());

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value("ACC-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions - should return 400 for missing required fields")
    @WithMockUser(roles = "USER")
    void createTransaction_invalidRequest_returns400() throws Exception {
        TransactionDTO.Request badRequest = new TransactionDTO.Request(); // empty

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} - should return 200 for existing transaction")
    @WithMockUser(roles = "USER")
    void getTransaction_returns200() throws Exception {
        when(transactionService.getTransaction(1L)).thenReturn(mockResponse());

        mockMvc.perform(get("/api/v1/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} - should return 401 for unauthenticated request")
    void getTransaction_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/transactions/{id}/reverse - should return 403 for USER role")
    @WithMockUser(roles = "USER")
    void reverseTransaction_forbiddenForUser() throws Exception {
        mockMvc.perform(patch("/api/v1/transactions/1/reverse").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/transactions/{id}/reverse - should return 200 for ADMIN role")
    @WithMockUser(roles = "ADMIN")
    void reverseTransaction_successForAdmin() throws Exception {
        TransactionDTO.Response reversed = mockResponse();
        when(transactionService.reverseTransaction(1L)).thenReturn(reversed);

        mockMvc.perform(patch("/api/v1/transactions/1/reverse").with(csrf()))
                .andExpect(status().isOk());
    }
}
