package com.prestloan.loanengine.dto;

import com.prestloan.loanengine.model.ScheduleStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
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
