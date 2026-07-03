package com.prestloan.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.prestloan.loanengine.model.Loan;
import com.prestloan.loanengine.model.LoanSchedule;
import com.prestloan.loanengine.model.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleGeneratorTest {

  private final ScheduleGenerator generator = new ScheduleGenerator();

  @Test
  void shouldGenerateExpectedInstallmentSequence() {
    Loan loan = new Loan();
    loan.setId(1L);
    loan.setAnnualInterestRate(new BigDecimal("12.0"));

    List<LoanSchedule> rows =
        generator.generate(
            loan,
            1,
            3,
            LocalDate.of(2026, 1, 1),
            new BigDecimal("1000"),
            new BigDecimal("340"),
            ScheduleStatus.PENDING);

    assertThat(rows).hasSize(3);
    assertThat(rows.get(0).getInstallmentNumber()).isEqualTo(1);
    assertThat(rows.get(1).getInstallmentNumber()).isEqualTo(2);
    assertThat(rows.get(2).getInstallmentNumber()).isEqualTo(3);
    assertThat(rows.get(2).getClosingBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
  }

  @Test
  void shouldCloseScheduleEarlyWhenBalanceSettles() {
    Loan loan = new Loan();
    loan.setId(2L);
    loan.setAnnualInterestRate(BigDecimal.ZERO);

    List<LoanSchedule> rows =
        generator.generate(
            loan,
            1,
            12,
            LocalDate.of(2026, 1, 1),
            new BigDecimal("100"),
            new BigDecimal("100"),
            ScheduleStatus.ADJUSTED);

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).getClosingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(rows.get(0).getStatus()).isEqualTo(ScheduleStatus.ADJUSTED);
  }
}
