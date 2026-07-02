package com.prestloan.loanengine.api.dto;

import com.prestloan.loanengine.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(
        Long id,
        BigDecimal principal,
        BigDecimal annualInterestRate,
        Integer tenureMonths,
        BigDecimal emi,
        LocalDate startDate,
        LoanStatus status
) {
}
