package com.prestloan.loanengine.service;

import com.prestloan.loanengine.dto.PrepaymentRequest;
import com.prestloan.loanengine.dto.PrepaymentResponse;
import com.prestloan.loanengine.model.Loan;
import com.prestloan.loanengine.model.LoanSchedule;
import com.prestloan.loanengine.model.PrepaymentOption;
import com.prestloan.loanengine.model.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReduceTenorKeepEmiComputation extends AbstractPrepaymentComputation {

    private final ScheduleGenerator scheduleGenerator;


    @Override
    public boolean supports(PrepaymentRequest request) {
        return request.option() == PrepaymentOption.REDUCE_TENOR_KEEP_EMI;
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

        if (after.compareTo(BigDecimal.ZERO) == 0) {
            schedules.removeIf(s -> s.getInstallmentNumber() > installment);
            return PrepaymentResponse.builder()
                    .loanId(loan.getId())
                    .option(request.option())
                    .installmentNumber(installment)
                    .prepaymentAmount(request.amount())
                    .outstandingBefore(before)
                    .outstandingAfter(after)
                    .newEmi(loan.getEmi())
                    .remainingTenorMonths(0)
                    .advancedInstallments(0)
                    .notes("Loan fully closed by prepayment")
                    .build();
        }

        BigDecimal monthlyRate = LoanMath.monthlyRate(loan.getAnnualInterestRate());
        int newTenor = LoanMath.calculateTenor(after, monthlyRate, loan.getEmi());

        List<LoanSchedule> newRows =
                scheduleGenerator.generate(
                        loan,
                        installment + 1,
                        newTenor,
                        snapshot.nextDueDate(),
                        after,
                        loan.getEmi(),
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
                .newEmi(loan.getEmi())
                .remainingTenorMonths(newTenor)
                .advancedInstallments(0)
                .notes("Tenor reduced, EMI unchanged")
                .build();
    }
}
