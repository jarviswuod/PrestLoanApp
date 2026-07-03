package com.prestloan.loanengine.service;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.PrepaymentOption;
import com.prestloan.loanengine.domain.ScheduleStatus;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdvanceInstallmentsComputation extends AbstractPrepaymentComputation {

  @Override
  public boolean supports(PrepaymentRequest request) {
    return request.option() == PrepaymentOption.ADVANCE_INSTALLMENTS;
  }

  @Override
  public PrepaymentResponse apply(
      Loan loan,
      PrepaymentSnapshot snapshot,
      List<LoanSchedule> schedules,
      PrepaymentRequest request) {
    int installment = snapshot.paidThroughInstallmentNumber();
    BigDecimal before = snapshot.outstandingPrincipal();

    BigDecimal pool = LoanMath.roundMoney(request.amount());
    int advanced = 0;

    List<LoanSchedule> futureRows =
        schedules.stream()
            .filter(s -> s.getInstallmentNumber() > installment)
            .sorted(Comparator.comparingInt(LoanSchedule::getInstallmentNumber))
            .toList();

    for (LoanSchedule row : futureRows) {
      if (pool.compareTo(row.getEmiAmount()) >= 0) {
        row.setStatus(ScheduleStatus.ADVANCED_PAID);
        pool = LoanMath.roundMoney(pool.subtract(row.getEmiAmount()));
        advanced++;
      } else {
        break;
      }
    }

    String notes = "Installment advance credit applied. Remaining credit: " + pool;
  return PrepaymentResponse.builder()
    .loanId(loan.getId())
    .option(request.option())
    .installmentNumber(installment)
    .prepaymentAmount(request.amount())
    .outstandingBefore(before)
    .outstandingAfter(before)
    .newEmi(loan.getEmi())
    .remainingTenorMonths(snapshot.remainingTermMonths())
    .advancedInstallments(advanced)
    .notes(notes)
    .build();
  }
}
