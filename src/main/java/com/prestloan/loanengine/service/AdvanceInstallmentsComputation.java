package com.prestloan.loanengine.service;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.PrepaymentOption;
import com.prestloan.loanengine.domain.ScheduleStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class AdvanceInstallmentsComputation extends AbstractPrepaymentComputation {

    @Override
    public boolean supports(PrepaymentRequest request) {
        return request.option() == PrepaymentOption.ADVANCE_INSTALLMENTS;
    }

    @Override
    public PrepaymentResponse apply(Loan loan, List<LoanSchedule> schedules, PrepaymentRequest request) {
        int installment = request.installmentNumber();
        BigDecimal before = outstandingBefore(schedules, installment);

        BigDecimal pool = LoanMath.roundMoney(request.amount());
        int advanced = 0;

        List<LoanSchedule> futureRows = schedules.stream()
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
        return new PrepaymentResponse(loan.getId(), request.option(), installment, request.amount(), before, before,
                loan.getEmi(), remainingMonths(loan, installment), advanced, notes);
    }
}
