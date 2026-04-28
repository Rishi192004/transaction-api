package com.rishi.transactionapi.service;

import com.rishi.transactionapi.dto.TransactionDTO;
import com.rishi.transactionapi.exception.DuplicateTransactionException;
import com.rishi.transactionapi.exception.TransactionNotFoundException;
import com.rishi.transactionapi.model.Transaction;
import com.rishi.transactionapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionDTO.Request validRequest;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        validRequest = TransactionDTO.Request.builder()
                .accountId("ACC-001")
                .amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.CREDIT)
                .description("Salary credit")
                .build();

        savedTransaction = Transaction.builder()
                .id(1L)
                .accountId("ACC-001")
                .amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.CREDIT)
                .description("Salary credit")
                .status(Transaction.TransactionStatus.COMPLETED)
                .idempotencyKey("test-key-123")
                .build();
        // Simulate @PrePersist
        savedTransaction.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("createTransaction: should create and return COMPLETED transaction")
    void createTransaction_success() {
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionDTO.Response response = transactionService.createTransaction(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccountId()).isEqualTo("ACC-001");
        assertThat(response.getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
        verify(transactionRepository, times(2)).save(any()); // PENDING then COMPLETED
    }

    @Test
    @DisplayName("createTransaction: should throw DuplicateTransactionException on same idempotency key")
    void createTransaction_duplicate_throwsException() {
        validRequest.setIdempotencyKey("duplicate-key");
        when(transactionRepository.findByIdempotencyKey("duplicate-key"))
                .thenReturn(Optional.of(savedTransaction));

        assertThatThrownBy(() -> transactionService.createTransaction(validRequest))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("duplicate-key");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getTransaction: should return transaction for valid id")
    void getTransaction_found() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(savedTransaction));

        TransactionDTO.Response response = transactionService.getTransaction(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("getTransaction: should throw TransactionNotFoundException for missing id")
    void getTransaction_notFound_throwsException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransaction(99L))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("reverseTransaction: should reverse a COMPLETED transaction")
    void reverseTransaction_success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(savedTransaction));
        Transaction reversed = Transaction.builder()
                .id(1L)
                .accountId("ACC-001")
                .amount(new BigDecimal("500.00"))
                .type(Transaction.TransactionType.CREDIT)
                .description("Salary credit")
                .status(Transaction.TransactionStatus.REVERSED)
                .idempotencyKey("test-key-123")
                .createdAt(LocalDateTime.now())
                .build();
        when(transactionRepository.save(any())).thenReturn(reversed);

        TransactionDTO.Response response = transactionService.reverseTransaction(1L);

        assertThat(response.getStatus()).isEqualTo(Transaction.TransactionStatus.REVERSED);
    }

    @Test
    @DisplayName("reverseTransaction: should throw when transaction is not COMPLETED")
    void reverseTransaction_notCompleted_throwsException() {
        savedTransaction.setStatus(Transaction.TransactionStatus.PENDING);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(savedTransaction));

        assertThatThrownBy(() -> transactionService.reverseTransaction(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }
}
