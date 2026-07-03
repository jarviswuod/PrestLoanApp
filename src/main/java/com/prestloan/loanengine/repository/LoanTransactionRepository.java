package com.prestloan.loanengine.repository;

import com.prestloan.loanengine.model.LoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanTransactionRepository extends JpaRepository<LoanTransaction, Long> {
}
