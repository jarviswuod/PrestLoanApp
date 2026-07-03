package com.prestloan.loanengine.service;

import com.prestloan.loanengine.dto.PrepaymentRequest;
import com.prestloan.loanengine.dto.PrepaymentResponse;
import com.prestloan.loanengine.model.Loan;
import com.prestloan.loanengine.model.LoanSchedule;

import java.util.List;

public class NoopPrepaymentComputation implements PrepaymentComputation {

    @Override
    public boolean supports(PrepaymentRequest request) {
        return false;
    }


    @Override
    public PrepaymentResponse apply(
            Loan loan,
            PrepaymentSnapshot snapshot,
            List<LoanSchedule> schedules,
            PrepaymentRequest request
    ) {
        throw new UnsupportedOperationException("Unsupported prepayment strategy");
    }
}
