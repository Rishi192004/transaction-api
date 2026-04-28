package com.rishi.transactionapi.controller;

import com.rishi.transactionapi.config.ratelimiter.RateLimited;
import com.rishi.transactionapi.dto.TransactionDTO;
import com.rishi.transactionapi.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @RateLimited("createTransaction")
    @Operation(summary = "Create a new transaction with idempotency support")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO.Response> createTransaction(
            @Valid @RequestBody TransactionDTO.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(request));
    }

    @GetMapping("/{id}")
    @RateLimited("getTransaction")
    @Operation(summary = "Get transaction by ID")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO.Response> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @GetMapping("/account/{accountId}")
    @RateLimited("getByAccount")
    @Operation(summary = "Get paginated transactions for an account")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO.PagedResponse> getByAccount(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                transactionService.getTransactionsByAccount(accountId, page, size));
    }

    @GetMapping("/account/{accountId}/range")
    @RateLimited("getByDateRange")
    @Operation(summary = "Get transactions filtered by date range")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO.PagedResponse> getByDateRange(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                transactionService.getTransactionsByDateRange(accountId, from, to, page, size));
    }

    @PatchMapping("/{id}/reverse")
    @RateLimited("reverseTransaction")
    @Operation(summary = "Reverse a completed transaction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO.Response> reverseTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.reverseTransaction(id));
    }
}
