package com.rishi.transactionapi.service;

import com.rishi.transactionapi.dto.TransactionDTO;
import com.rishi.transactionapi.exception.DuplicateTransactionException;
import com.rishi.transactionapi.exception.TransactionNotFoundException;
import com.rishi.transactionapi.model.Transaction;
import com.rishi.transactionapi.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionDTO.Response createTransaction(TransactionDTO.Request request) {
        // Idempotency: generate key if not supplied by client
        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        // Check for duplicate — return existing result instead of re-processing
        transactionRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            log.warn("Duplicate transaction detected for idempotency key: {}", idempotencyKey);
            throw new DuplicateTransactionException(idempotencyKey, existing.getId());
        });

        Transaction transaction = Transaction.builder()
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // Simulate processing — in real system, publish to SQS/Kafka for async settlement
        saved.setStatus(Transaction.TransactionStatus.COMPLETED);
        saved = transactionRepository.save(saved);

        log.info("Transaction created: id={}, account={}, type={}, amount={}",
                saved.getId(), saved.getAccountId(), saved.getType(), saved.getAmount());

        return TransactionDTO.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public TransactionDTO.Response getTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return TransactionDTO.Response.from(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionDTO.PagedResponse getTransactionsByAccount(
            String accountId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> result = transactionRepository.findByAccountId(accountId, pageable);

        return TransactionDTO.PagedResponse.builder()
                .transactions(result.getContent().stream()
                        .map(TransactionDTO.Response::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public TransactionDTO.PagedResponse getTransactionsByDateRange(
            String accountId, LocalDateTime from, LocalDateTime to, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transaction> result = transactionRepository
                .findByAccountIdAndDateRange(accountId, from, to, pageable);

        return TransactionDTO.PagedResponse.builder()
                .transactions(result.getContent().stream()
                        .map(TransactionDTO.Response::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public TransactionDTO.Response reverseTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        if (transaction.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only COMPLETED transactions can be reversed. Current status: " + transaction.getStatus());
        }

        transaction.setStatus(Transaction.TransactionStatus.REVERSED);
        Transaction reversed = transactionRepository.save(transaction);

        log.info("Transaction reversed: id={}", reversed.getId());
        return TransactionDTO.Response.from(reversed);
    }
}
