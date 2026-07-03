package com.prestloan.loanengine.api.dto;

import com.prestloan.loanengine.domain.PrepaymentOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(
    description =
        "Category A prepayment request. installmentNumber represents the last fully paid installment after which the prepayment is applied.")
public record PrepaymentRequest(
    @Schema(
            description =
                "Last fully paid installment number after which the prepayment is applied")
        @NotNull(message = "installmentNumber is required") @Min(value = 1, message = "installmentNumber must be at least 1")
        Integer installmentNumber,
    @NotNull(message = "amount is required") @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "amount supports up to 17 integer digits and 2 decimal places")
        BigDecimal amount,
    @NotNull(message = "option is required") PrepaymentOption option) {}
