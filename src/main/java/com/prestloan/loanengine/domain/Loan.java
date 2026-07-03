package com.prestloan.loanengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "loans")
@SQLDelete(
    sql =
        "UPDATE loans SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id=?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
public class Loan extends BaseEntity {

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal principal;

  @Column(name = "annual_interest_rate", nullable = false, precision = 9, scale = 6)
  private BigDecimal annualInterestRate;

  @Column(name = "tenure_months", nullable = false)
  private Integer tenureMonths;

  @Column(name = "original_tenure_months", nullable = false)
  private Integer originalTenureMonths;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal emi;

  @Column(name = "original_emi", nullable = false, precision = 19, scale = 2)
  private BigDecimal originalEmi;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LoanStatus status = LoanStatus.ACTIVE;
}
