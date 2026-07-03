package com.prestloan.loanengine.repository;

import com.prestloan.loanengine.model.LoanSchedule;
import com.prestloan.loanengine.model.ScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanScheduleRepository extends JpaRepository<LoanSchedule, Long> {

    List<LoanSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    @Query(
    """
        select s from LoanSchedule s
        where s.loan.id = :loanId
            and (:status is null or s.status = :status)
            and (:fromInstallment is null or s.installmentNumber >= :fromInstallment)
            and (:toInstallment is null or s.installmentNumber <= :toInstallment)
        """)
    Page<LoanSchedule> searchByLoanIdAndFilters(
            @Param("loanId") Long loanId,
            @Param("status") ScheduleStatus status,
            @Param("fromInstallment") Integer fromInstallment,
            @Param("toInstallment") Integer toInstallment,
            Pageable pageable);
}
