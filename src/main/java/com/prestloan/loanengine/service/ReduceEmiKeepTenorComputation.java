package com.prestloan.loanengine.service;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.PrepaymentOption;
import com.prestloan.loanengine.domain.ScheduleStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReduceEmiKeepTenorComputation extends AbstractPrepaymentComputation {

  private final ScheduleGenerator scheduleGenerator;

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
    int installment = snapshot.paidThroughInstallmentNumber();
    BigDecimal before = snapshot.outstandingPrincipal();
    BigDecimal after = LoanMath.roundMoney(before.subtract(request.amount()));
    int remaining = snapshot.remainingTermMonths();

    if (after.compareTo(BigDecimal.ZERO) == 0 || remaining == 0) {
      return PrepaymentResponse.builder()
          .loanId(loan.getId())
          .option(request.option())
          .installmentNumber(installment)
          .prepaymentAmount(request.amount())
          .outstandingBefore(before)
          .outstandingAfter(after)
          .newEmi(BigDecimal.ZERO)
          .remainingTenorMonths(0)
          .advancedInstallments(0)
          .notes("Loan fully closed by prepayment")
          .build();
    }

    BigDecimal monthlyRate = LoanMath.monthlyRate(loan.getAnnualInterestRate());
    BigDecimal newEmi = LoanMath.calculateEmi(after, monthlyRate, remaining);

    List<LoanSchedule> newRows =
        scheduleGenerator.generate(
            loan,
            installment + 1,
            remaining,
            snapshot.nextDueDate(),
            after,
            newEmi,
            ScheduleStatus.ADJUSTED);

    schedules.removeIf(s -> s.getInstallmentNumber() > installment);
    schedules.addAll(newRows);
    sortByInstallment(schedules);

    return PrepaymentResponse.builder()
      .loanId(loan.getId())
      .option(request.option())
      .installmentNumber(installment)
      .prepaymentAmount(request.amount())
      .outstandingBefore(before)
      .outstandingAfter(after)
      .newEmi(newEmi)
      .remainingTenorMonths(remaining)
      .advancedInstallments(0)
      .notes("EMI recalculated, tenor unchanged")
      .build();
  }
}
