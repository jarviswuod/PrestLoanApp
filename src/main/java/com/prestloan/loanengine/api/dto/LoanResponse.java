package com.prestloan.loanengine.api.dto;

import com.prestloan.loanengine.domain.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

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
        LoanStatus status) {
}
