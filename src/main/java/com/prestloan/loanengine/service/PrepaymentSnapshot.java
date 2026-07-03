package com.prestloan.loanengine.service;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record PrepaymentSnapshot(
        int paidThroughInstallmentNumber,
        BigDecimal outstandingPrincipal,
        int remainingTermMonths,
        LocalDate nextDueDate) {
}
