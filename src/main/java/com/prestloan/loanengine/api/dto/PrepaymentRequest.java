package com.prestloan.loanengine.api.dto;

import com.prestloan.loanengine.domain.PrepaymentOption;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Category A prepayment request")
public record PrepaymentRequest(
        @NotNull @Min(1) Integer installmentNumber,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull PrepaymentOption option
) {
}
