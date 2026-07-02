package com.prestloan.loanengine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request payload to create a base loan and generate full initial schedule")
public record CreateLoanRequest(
        @NotNull @DecimalMin("0.01") BigDecimal principal,
        @NotNull @DecimalMin("0.000001") BigDecimal annualInterestRate,
        @NotNull @Min(1) Integer tenureMonths,
        @NotNull LocalDate startDate
) {
}
