package com.prestloan.loanengine.dto;

import com.prestloan.loanengine.model.LoanStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record LoanResponse(
        Long id,
        BigDecimal principal,
        BigDecimal annualInterestRate,
        Integer originalTenureMonths,
        Integer tenureMonths,
        BigDecimal originalEmi,
        BigDecimal emi,
        LocalDate startDate,
        LoanStatus status
) {
}
