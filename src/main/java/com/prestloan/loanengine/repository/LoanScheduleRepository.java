package com.prestloan.loanengine.repository;

import com.prestloan.loanengine.domain.LoanSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {

    List<LoanSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    Optional<LoanSchedule> findByLoanIdAndInstallmentNumber(Long loanId, Integer installmentNumber);
}
