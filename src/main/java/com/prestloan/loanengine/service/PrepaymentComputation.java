package com.prestloan.loanengine.service;

import com.prestloan.loanengine.dto.PrepaymentRequest;
import com.prestloan.loanengine.dto.PrepaymentResponse;
import com.prestloan.loanengine.model.Loan;
import com.prestloan.loanengine.model.LoanSchedule;

import java.util.List;

public interface PrepaymentComputation {

    boolean supports(PrepaymentRequest request);

    PrepaymentResponse apply(
            Loan loan,
            PrepaymentSnapshot snapshot,
            List<LoanSchedule> schedules,
            PrepaymentRequest request);
}
