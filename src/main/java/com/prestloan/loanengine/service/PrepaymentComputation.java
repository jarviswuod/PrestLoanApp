package com.prestloan.loanengine.service;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import java.util.List;

public interface PrepaymentComputation {

  boolean supports(PrepaymentRequest request);

  PrepaymentResponse apply(
      Loan loan,
      PrepaymentSnapshot snapshot,
      List<LoanSchedule> schedules,
      PrepaymentRequest request);
}
