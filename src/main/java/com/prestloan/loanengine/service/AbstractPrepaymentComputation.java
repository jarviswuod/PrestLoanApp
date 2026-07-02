package com.prestloan.loanengine.service;

import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractPrepaymentComputation implements PrepaymentComputation {

    protected LoanSchedule installmentRow(List<LoanSchedule> schedules, int installmentNumber) {
        return schedules.stream()
                .filter(s -> s.getInstallmentNumber() == installmentNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Installment number not found for loan"));
    }

    protected BigDecimal outstandingBefore(List<LoanSchedule> schedules, int installmentNumber) {
        return installmentRow(schedules, installmentNumber).getClosingBalance();
    }

    protected int remainingMonths(Loan loan, int installmentNumber) {
        return loan.getTenureMonths() - installmentNumber;
    }

    protected LocalDate nextDueDate(Loan loan, List<LoanSchedule> schedules, int installmentNumber) {
        return schedules.stream()
                .filter(s -> s.getInstallmentNumber() == installmentNumber + 1)
                .map(LoanSchedule::getDueDate)
                .findFirst()
                .orElse(loan.getStartDate().plusMonths(installmentNumber));
    }

    protected List<LoanSchedule> sortByInstallment(List<LoanSchedule> schedules) {
        schedules.sort(Comparator.comparingInt(LoanSchedule::getInstallmentNumber));
        return schedules;
    }
}
