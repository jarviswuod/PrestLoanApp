package com.prestloan.loanengine.api.dto;

import com.prestloan.loanengine.domain.ScheduleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleRowResponse(
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal openingBalance,
        BigDecimal emiAmount,
        BigDecimal principalComponent,
        BigDecimal interestComponent,
        BigDecimal closingBalance,
        ScheduleStatus status
) {
}
