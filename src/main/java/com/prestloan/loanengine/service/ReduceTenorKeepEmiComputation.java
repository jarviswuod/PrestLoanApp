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
public class ReduceTenorKeepEmiComputation extends AbstractPrepaymentComputation {

    private final ScheduleGenerator scheduleGenerator;

    public ReduceTenorKeepEmiComputation(ScheduleGenerator scheduleGenerator) {
        this.scheduleGenerator = scheduleGenerator;
    }

    @Override
    public boolean supports(PrepaymentRequest request) {
        return request.option() == PrepaymentOption.REDUCE_TENOR_KEEP_EMI;
    }

    @Override
    public PrepaymentResponse apply(Loan loan, List<LoanSchedule> schedules, PrepaymentRequest request) {
        int installment = request.installmentNumber();
        BigDecimal before = outstandingBefore(schedules, installment);
        BigDecimal after = LoanMath.roundMoney(before.subtract(request.amount()));

        if (after.compareTo(BigDecimal.ZERO) == 0) {
            schedules.removeIf(s -> s.getInstallmentNumber() > installment);
            return new PrepaymentResponse(loan.getId(), request.option(), installment, request.amount(), before, after,
                    loan.getEmi(), 0, 0, "Loan fully closed by prepayment");
        }

        BigDecimal monthlyRate = LoanMath.monthlyRate(loan.getAnnualInterestRate());
        int newTenor = LoanMath.calculateTenor(after, monthlyRate, loan.getEmi());

        List<LoanSchedule> newRows = scheduleGenerator.generate(
                loan,
                installment + 1,
                newTenor,
                nextDueDate(loan, schedules, installment),
                after,
                loan.getEmi(),
                ScheduleStatus.ADJUSTED
        );

        schedules.removeIf(s -> s.getInstallmentNumber() > installment);
        schedules.addAll(newRows);
        sortByInstallment(schedules);

        return new PrepaymentResponse(loan.getId(), request.option(), installment, request.amount(), before, after,
                loan.getEmi(), newTenor, 0, "Tenor reduced, EMI unchanged");
    }
}
