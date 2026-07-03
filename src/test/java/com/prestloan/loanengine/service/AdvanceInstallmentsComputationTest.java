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

class AdvanceInstallmentsComputationTest {

  private final AdvanceInstallmentsComputation computation = new AdvanceInstallmentsComputation();

  @Test
  void shouldAdvanceInstallmentsWithoutRecalculation() {
    Loan loan = new Loan();
    loan.setId(1L);
    loan.setEmi(new BigDecimal("10000.00"));

    LoanSchedule row25 = row(25, "10000.00");
    LoanSchedule row26 = row(26, "10000.00");
    LoanSchedule row27 = row(27, "10000.00");

    List<LoanSchedule> schedules = new ArrayList<>(List.of(row25, row26, row27));

    PrepaymentSnapshot snapshot =
        new PrepaymentSnapshot(24, new BigDecimal("500000.00"), 36, LocalDate.of(2028, 1, 1));
    PrepaymentRequest request =
        new PrepaymentRequest(
            24, new BigDecimal("25000.00"), PrepaymentOption.ADVANCE_INSTALLMENTS);

    PrepaymentResponse response = computation.apply(loan, snapshot, schedules, request);

    assertThat(response.outstandingAfter()).isEqualByComparingTo("500000.00");
    assertThat(response.advancedInstallments()).isEqualTo(2);
    assertThat(schedules.get(0).getStatus()).isEqualTo(ScheduleStatus.ADVANCED_PAID);
    assertThat(schedules.get(1).getStatus()).isEqualTo(ScheduleStatus.ADVANCED_PAID);
    assertThat(schedules.get(2).getStatus()).isEqualTo(ScheduleStatus.PENDING);
  }

  private LoanSchedule row(int installment, String emi) {
    LoanSchedule row = new LoanSchedule();
    row.setInstallmentNumber(installment);
    row.setEmiAmount(new BigDecimal(emi));
    row.setStatus(ScheduleStatus.PENDING);
    return row;
  }
}
