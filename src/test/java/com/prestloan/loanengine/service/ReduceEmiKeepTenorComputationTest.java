package com.prestloan.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.PrepaymentOption;
import com.prestloan.loanengine.domain.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReduceEmiKeepTenorComputationTest {

  private final ScheduleGenerator scheduleGenerator = new ScheduleGenerator();
  private final ReduceEmiKeepTenorComputation computation =
      new ReduceEmiKeepTenorComputation(scheduleGenerator);

  @Test
  void shouldReduceEmiAndKeepRemainingTenor() {
    Loan loan = new Loan();
    loan.setPrincipal(new BigDecimal("1000000"));
    loan.setAnnualInterestRate(new BigDecimal("12.0"));
    loan.setTenureMonths(60);
    loan.setStartDate(LocalDate.of(2026, 1, 1));
    loan.setEmi(new BigDecimal("22244.45"));

    List<com.prestloan.loanengine.domain.LoanSchedule> schedules =
        new ArrayList<>(
            scheduleGenerator.generate(
                loan,
                1,
                60,
                loan.getStartDate(),
                loan.getPrincipal(),
                loan.getEmi(),
                ScheduleStatus.PENDING));

    PrepaymentSnapshot snapshot =
        new PrepaymentSnapshot(
            24, schedules.get(23).getClosingBalance(), 36, schedules.get(24).getDueDate());

    PrepaymentResponse result =
        computation.apply(
            loan,
            snapshot,
            schedules,
            new PrepaymentRequest(
                24, new BigDecimal("200000"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR));

    assertEquals(36, result.remainingTenorMonths());
    assertTrue(result.newEmi().compareTo(new BigDecimal("22244.45")) < 0);
  }

  @Test
  void shouldCloseLoanWhenPrepaymentFullySettlesOutstanding() {
    Loan loan = new Loan();
    loan.setId(100L);
    loan.setAnnualInterestRate(new BigDecimal("12.0"));

    List<com.prestloan.loanengine.domain.LoanSchedule> schedules = new ArrayList<>();
    PrepaymentSnapshot snapshot =
        new PrepaymentSnapshot(24, new BigDecimal("200000.00"), 36, LocalDate.of(2028, 1, 1));

    PrepaymentResponse result =
        computation.apply(
            loan,
            snapshot,
            schedules,
            new PrepaymentRequest(
                24, new BigDecimal("200000.00"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR));

    assertThat(result.outstandingAfter()).isEqualByComparingTo("0.00");
    assertThat(result.newEmi()).isEqualByComparingTo("0");
    assertThat(result.remainingTenorMonths()).isEqualTo(0);
  }
}
