package com.prestloan.loanengine.service;

import com.prestloan.loanengine.model.Loan;
import com.prestloan.loanengine.model.LoanSchedule;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractPrepaymentComputation implements PrepaymentComputation {

    protected LoanSchedule paidInstallmentRow(
            List<LoanSchedule> schedules,
            int installmentNumber
    ) {
        return schedules.stream()
                .filter(s -> s.getInstallmentNumber() == installmentNumber)
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException("Paid-through installment number not found for loan"));
    }


    protected LocalDate nextDueDate(
            Loan loan,
            List<LoanSchedule> schedules,
            int installmentNumber
    ) {
        return schedules.stream()
                .filter(s -> s.getInstallmentNumber() == installmentNumber + 1)
                .map(LoanSchedule::getDueDate)
                .findFirst()
                .orElse(loan.getStartDate().plusMonths(installmentNumber));
    }


    protected PrepaymentSnapshot snapshot(
            Loan loan,
            List<LoanSchedule> schedules,
            int installmentNumber
    ) {
        LoanSchedule paidRow = paidInstallmentRow(schedules, installmentNumber);
        return PrepaymentSnapshot.builder()
                .paidThroughInstallmentNumber(installmentNumber)
                .outstandingPrincipal(paidRow.getClosingBalance())
                .remainingTermMonths(loan.getTenureMonths() - installmentNumber)
                .nextDueDate(nextDueDate(loan, schedules, installmentNumber))
                .build();
    }


    protected List<LoanSchedule> sortByInstallment(
            List<LoanSchedule> schedules
    ) {
        schedules.sort(Comparator.comparingInt(LoanSchedule::getInstallmentNumber));
        return schedules;
    }
}
