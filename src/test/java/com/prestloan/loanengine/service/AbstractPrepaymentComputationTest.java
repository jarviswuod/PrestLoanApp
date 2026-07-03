package com.prestloan.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.PrepaymentOption;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AbstractPrepaymentComputationTest {

  private final TestComputation computation = new TestComputation();

  @Test
  void shouldBuildSnapshotUsingNextInstallmentDueDate() {
    Loan loan = new Loan();
    loan.setStartDate(LocalDate.of(2026, 1, 1));
    loan.setTenureMonths(60);

    LoanSchedule row24 = new LoanSchedule();
    row24.setInstallmentNumber(24);
    row24.setClosingBalance(new BigDecimal("500000"));

    LoanSchedule row25 = new LoanSchedule();
    row25.setInstallmentNumber(25);
    row25.setDueDate(LocalDate.of(2028, 1, 1));

    PrepaymentSnapshot snapshot =
        computation.snapshot(loan, new ArrayList<>(List.of(row24, row25)), 24);

    assertThat(snapshot.nextDueDate()).isEqualTo(LocalDate.of(2028, 1, 1));
    assertThat(snapshot.remainingTermMonths()).isEqualTo(36);
  }

  @Test
  void shouldFallbackToDerivedNextDueDateWhenMissingNextInstallment() {
    Loan loan = new Loan();
    loan.setStartDate(LocalDate.of(2026, 1, 1));
    loan.setTenureMonths(60);

    LoanSchedule row24 = new LoanSchedule();
    row24.setInstallmentNumber(24);
    row24.setClosingBalance(new BigDecimal("500000"));

    PrepaymentSnapshot snapshot = computation.snapshot(loan, new ArrayList<>(List.of(row24)), 24);

    assertThat(snapshot.nextDueDate()).isEqualTo(LocalDate.of(2028, 1, 1));
  }

  @Test
  void shouldThrowWhenPaidThroughInstallmentMissing() {
    LoanSchedule row10 = new LoanSchedule();
    row10.setInstallmentNumber(10);

    assertThatThrownBy(() -> computation.paidInstallmentRow(new ArrayList<>(List.of(row10)), 24))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Paid-through installment number not found");
  }

  @Test
  void shouldSortRowsByInstallment() {
    LoanSchedule row3 = new LoanSchedule();
    row3.setInstallmentNumber(3);
    LoanSchedule row1 = new LoanSchedule();
    row1.setInstallmentNumber(1);
    LoanSchedule row2 = new LoanSchedule();
    row2.setInstallmentNumber(2);

    List<LoanSchedule> sorted =
        computation.sortByInstallment(new ArrayList<>(List.of(row3, row1, row2)));

    assertThat(sorted.get(0).getInstallmentNumber()).isEqualTo(1);
    assertThat(sorted.get(1).getInstallmentNumber()).isEqualTo(2);
    assertThat(sorted.get(2).getInstallmentNumber()).isEqualTo(3);
  }

  private static class TestComputation extends AbstractPrepaymentComputation {
    @Override
    public boolean supports(PrepaymentRequest request) {
      return request.option() == PrepaymentOption.REDUCE_EMI_KEEP_TENOR;
    }

    @Override
    public PrepaymentResponse apply(
        Loan loan,
        PrepaymentSnapshot snapshot,
        List<LoanSchedule> schedules,
        PrepaymentRequest request) {
      return null;
    }
  }
}
