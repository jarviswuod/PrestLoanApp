package com.prestloan.loanengine.service;

import com.prestloan.loanengine.dto.PrepaymentRequest;
import com.prestloan.loanengine.dto.PrepaymentResponse;
import com.prestloan.loanengine.model.Loan;
import com.prestloan.loanengine.model.LoanSchedule;
import com.prestloan.loanengine.model.PrepaymentOption;
import com.prestloan.loanengine.model.ScheduleStatus;
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
    public PrepaymentResponse apply(
            Loan loan,
            PrepaymentSnapshot snapshot,
            List<LoanSchedule> schedules,
            PrepaymentRequest request
    ) {
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
