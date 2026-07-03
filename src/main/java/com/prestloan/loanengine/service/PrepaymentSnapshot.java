package com.prestloan.loanengine.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record PrepaymentSnapshot(
    int paidThroughInstallmentNumber,
    BigDecimal outstandingPrincipal,
    int remainingTermMonths,
    LocalDate nextDueDate) {}
