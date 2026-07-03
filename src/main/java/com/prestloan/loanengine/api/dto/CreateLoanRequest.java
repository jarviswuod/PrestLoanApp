package com.prestloan.loanengine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request payload to create a base loan and generate full initial schedule")
public record CreateLoanRequest(
    @NotNull(message = "principal is required") @DecimalMin(value = "0.01", message = "principal must be greater than 0")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "principal supports up to 17 integer digits and 2 decimal places")
        BigDecimal principal,
    @NotNull(message = "annualInterestRate is required") @DecimalMin(value = "0.00", message = "annualInterestRate cannot be negative")
        @Digits(
            integer = 3,
            fraction = 6,
            message = "annualInterestRate supports up to 3 integer digits and 6 decimal places")
        BigDecimal annualInterestRate,
    @NotNull(message = "tenureMonths is required") @Min(value = 1, message = "tenureMonths must be at least 1")
        Integer tenureMonths,
    @NotNull(message = "startDate is required") LocalDate startDate) {}
