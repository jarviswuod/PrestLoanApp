package com.prestloan.loanengine.repository;

import com.prestloan.loanengine.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
}
