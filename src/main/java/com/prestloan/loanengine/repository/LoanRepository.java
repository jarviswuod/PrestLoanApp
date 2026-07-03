package com.prestloan.loanengine.repository;

import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from Loan l where l.id = :id")
  Optional<Loan> findByIdForUpdate(@Param("id") Long id);

  @Query(
      """
			select l from Loan l
			where (:status is null or l.status = :status)
			  and (:startDateFrom is null or l.startDate >= :startDateFrom)
			  and (:startDateTo is null or l.startDate <= :startDateTo)
		""")
  Page<Loan> searchByFilters(
      @Param("status") LoanStatus status,
      @Param("startDateFrom") LocalDate startDateFrom,
      @Param("startDateTo") LocalDate startDateTo,
      Pageable pageable);
}
