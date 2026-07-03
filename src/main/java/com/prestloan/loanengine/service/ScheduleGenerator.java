package com.prestloan.loanengine.service;

import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduleGenerator {

  public List<LoanSchedule> generate(
      Loan loan,
      int startInstallmentNumber,
      int months,
      LocalDate firstDueDate,
      BigDecimal startingBalance,
      BigDecimal emi,
      ScheduleStatus status) {
    log.debug(
        "Generating schedule: loanId={}, startInstallment={}, months={}, status={}",
        loan.getId(),
        startInstallmentNumber,
        months,
        status);

    List<LoanSchedule> rows = new ArrayList<>();
    BigDecimal balance = LoanMath.roundMoney(startingBalance);
    BigDecimal monthlyRate = LoanMath.monthlyRate(loan.getAnnualInterestRate());

    for (int i = 0; i < months; i++) {
      int installment = startInstallmentNumber + i;
      LocalDate dueDate = firstDueDate.plusMonths(i);

      BigDecimal interest = LoanMath.roundMoney(balance.multiply(monthlyRate));
      BigDecimal principal = LoanMath.roundMoney(emi.subtract(interest));

      if (principal.compareTo(balance) > 0) {
        principal = balance;
      }

      BigDecimal rowEmi = LoanMath.roundMoney(principal.add(interest));
      BigDecimal closing = LoanMath.roundMoney(balance.subtract(principal));

      LoanSchedule schedule = new LoanSchedule();
      schedule.setLoan(loan);
      schedule.setInstallmentNumber(installment);
      schedule.setDueDate(dueDate);
      schedule.setOpeningBalance(balance);
      schedule.setEmiAmount(rowEmi);
      schedule.setPrincipalComponent(principal);
      schedule.setInterestComponent(interest);
      schedule.setClosingBalance(closing.max(BigDecimal.ZERO));
      schedule.setStatus(status);
      rows.add(schedule);

      balance = closing.max(BigDecimal.ZERO);
      if (balance.compareTo(BigDecimal.ZERO) == 0) {
        break;
      }
    }

    log.debug(
        "Schedule generation completed: loanId={}, generatedRows={}", loan.getId(), rows.size());
    return rows;
  }
}
