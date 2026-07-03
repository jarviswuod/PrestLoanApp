package com.prestloan.loanengine.dto;

import com.prestloan.loanengine.model.PrepaymentOption;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
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
