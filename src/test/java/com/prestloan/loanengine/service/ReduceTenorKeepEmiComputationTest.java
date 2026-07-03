package com.prestloan.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.PrepaymentOption;
import com.prestloan.loanengine.domain.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReduceTenorKeepEmiComputationTest {

  private final ScheduleGenerator scheduleGenerator = new ScheduleGenerator();
  private final ReduceTenorKeepEmiComputation computation =
      new ReduceTenorKeepEmiComputation(scheduleGenerator);

  @Test
  void shouldReduceTenorAndKeepEmi() {
    Loan loan = new Loan();
    loan.setId(1L);
    loan.setAnnualInterestRate(new BigDecimal("12.0"));
    loan.setEmi(new BigDecimal("22244.45"));
    loan.setTenureMonths(60);
    loan.setStartDate(LocalDate.of(2026, 1, 1));

    LoanSchedule paid = new LoanSchedule();
    paid.setInstallmentNumber(24);
    paid.setClosingBalance(new BigDecimal("669724.76"));
    paid.setDueDate(LocalDate.of(2027, 12, 1));

    LoanSchedule next = new LoanSchedule();
    next.setInstallmentNumber(25);
    next.setDueDate(LocalDate.of(2028, 1, 1));

    List<LoanSchedule> schedules = new ArrayList<>(List.of(paid, next));

    PrepaymentSnapshot snapshot =
        new PrepaymentSnapshot(24, new BigDecimal("669724.76"), 36, LocalDate.of(2028, 1, 1));
    PrepaymentRequest request =
        new PrepaymentRequest(24, new BigDecimal("200000"), PrepaymentOption.REDUCE_TENOR_KEEP_EMI);

    PrepaymentResponse response = computation.apply(loan, snapshot, schedules, request);

    assertThat(response.newEmi()).isEqualByComparingTo("22244.45");
    assertThat(response.remainingTenorMonths()).isLessThan(36);
    assertThat(schedules)
        .allMatch(s -> s.getInstallmentNumber() <= 24 || s.getStatus() == ScheduleStatus.ADJUSTED);
  }
}
