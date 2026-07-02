package com.prestloan.loanengine.service;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.PrepaymentOption;
import com.prestloan.loanengine.domain.ScheduleStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ReduceEmiKeepTenorComputation extends AbstractPrepaymentComputation {

    private final ScheduleGenerator scheduleGenerator;

    public ReduceEmiKeepTenorComputation(ScheduleGenerator scheduleGenerator) {
        this.scheduleGenerator = scheduleGenerator;
    }

    @Override
    public boolean supports(PrepaymentRequest request) {
        return request.option() == PrepaymentOption.REDUCE_EMI_KEEP_TENOR;
    }

    @Override
    public PrepaymentResponse apply(Loan loan, List<LoanSchedule> schedules, PrepaymentRequest request) {
        int installment = request.installmentNumber();
        BigDecimal before = outstandingBefore(schedules, installment);
        BigDecimal after = LoanMath.roundMoney(before.subtract(request.amount()));
        int remaining = remainingMonths(loan, installment);

        if (after.compareTo(BigDecimal.ZERO) == 0 || remaining == 0) {
            return new PrepaymentResponse(loan.getId(), request.option(), installment, request.amount(), before, after,
                    BigDecimal.ZERO, 0, 0, "Loan fully closed by prepayment");
        }

        BigDecimal monthlyRate = LoanMath.monthlyRate(loan.getAnnualInterestRate());
        BigDecimal newEmi = LoanMath.calculateEmi(after, monthlyRate, remaining);

        List<LoanSchedule> newRows = scheduleGenerator.generate(
                loan,
                installment + 1,
                remaining,
                nextDueDate(loan, schedules, installment),
                after,
                newEmi,
                ScheduleStatus.ADJUSTED
        );

        schedules.removeIf(s -> s.getInstallmentNumber() > installment);
        schedules.addAll(newRows);
        sortByInstallment(schedules);

        return new PrepaymentResponse(loan.getId(), request.option(), installment, request.amount(), before, after,
                newEmi, remaining, 0, "EMI recalculated, tenor unchanged");
    }
}
