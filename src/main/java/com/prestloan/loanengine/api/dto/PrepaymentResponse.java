package com.prestloan.loanengine.api.dto;

import com.prestloan.loanengine.domain.PrepaymentOption;

import java.math.BigDecimal;

public record PrepaymentResponse(
        Long loanId,
        PrepaymentOption option,
        Integer installmentNumber,
        BigDecimal prepaymentAmount,
        BigDecimal outstandingBefore,
        BigDecimal outstandingAfter,
        BigDecimal newEmi,
        Integer remainingTenorMonths,
        Integer advancedInstallments,
        String notes
) {
}
