package com.prestloan.loanengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "loan_schedules")
@SQLDelete(
    sql =
        "UPDATE loan_schedules SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id=?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
public class LoanSchedule extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "loan_id", nullable = false)
  private Loan loan;

  @Column(name = "installment_number", nullable = false)
  private Integer installmentNumber;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal openingBalance;

  @Column(name = "emi_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal emiAmount;

  @Column(name = "principal_component", nullable = false, precision = 19, scale = 2)
  private BigDecimal principalComponent;

  @Column(name = "interest_component", nullable = false, precision = 19, scale = 2)
  private BigDecimal interestComponent;

  @Column(name = "closing_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal closingBalance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScheduleStatus status = ScheduleStatus.PENDING;
}
