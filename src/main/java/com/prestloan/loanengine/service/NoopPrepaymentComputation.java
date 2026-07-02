package com.prestloan.loanengine.service;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;

import java.util.List;

public class NoopPrepaymentComputation implements PrepaymentComputation {

    @Override
    public boolean supports(PrepaymentRequest request) {
        return false;
    }

    @Override
    public PrepaymentResponse apply(Loan loan, List<LoanSchedule> schedules, PrepaymentRequest request) {
        throw new UnsupportedOperationException("Unsupported prepayment strategy");
    }
}
