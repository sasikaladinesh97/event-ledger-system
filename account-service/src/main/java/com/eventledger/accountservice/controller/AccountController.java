package com.eventledger.accountservice.controller;

import com.eventledger.accountservice.model.Transaction;
import com.eventledger.accountservice.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService service;
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    public AccountController(AccountService service) {
        this.service = service;
    }

    // Apply a transaction to an account
    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<Transaction> applyTransaction(@PathVariable String accountId,
                                                        @Valid @RequestBody Transaction tx,
                                                        @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        tx.setAccountId(accountId);
        Transaction saved = service.applyTransaction(tx);
        logger.info("traceId={} Applied transaction {} for account {}", traceId, tx.getEventId(), accountId);
        return ResponseEntity.ok(saved);
    }

    // Get current balance for an account
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Double> getBalance(@PathVariable String accountId,
                                             @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        double balance = service.getBalance(accountId);
        logger.info("traceId={} Retrieved balance {} for account {}", traceId, balance, accountId);
        return ResponseEntity.ok(balance);
    }

    // Get account details and recent transactions
    @GetMapping("/{accountId}")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable String accountId,
                                                             @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        List<Transaction> txs = service.getTransactions(accountId);
        logger.info("traceId={} Retrieved {} transactions for account {}", traceId, txs.size(), accountId);
        return ResponseEntity.ok(txs);
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Account Service is healthy");
    }
}
