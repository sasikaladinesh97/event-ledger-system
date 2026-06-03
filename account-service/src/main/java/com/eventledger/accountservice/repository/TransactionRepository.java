package com.eventledger.accountservice.repository;

import com.eventledger.accountservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByAccountIdOrderByEventTimestampAsc(String accountId);
}
