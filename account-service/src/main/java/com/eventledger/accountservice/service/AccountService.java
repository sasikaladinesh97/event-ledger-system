package com.eventledger.accountservice.service;

import com.eventledger.accountservice.model.Transaction;
import com.eventledger.accountservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AccountService {
    private final TransactionRepository repo;

    public AccountService(TransactionRepository repo) {
        this.repo = repo;
    }

    public Transaction applyTransaction(Transaction tx) {
        return repo.save(tx);
    }

    public double getBalance(String accountId) {
        List<Transaction> txs = repo.findByAccountIdOrderByEventTimestampAsc(accountId);
        return txs.stream()
                .mapToDouble(t -> t.getType().equals("CREDIT") ? t.getAmount() : -t.getAmount())
                .sum();
    }

    public List<Transaction> getTransactions(String accountId) {
        return repo.findByAccountIdOrderByEventTimestampAsc(accountId);
    }
}
