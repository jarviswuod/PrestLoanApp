package com.prestloan.loanengine.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanMathTest {

    @Test
    void shouldCalculateExpectedEmiForBaseLoan() {
        BigDecimal monthlyRate = LoanMath.monthlyRate(new BigDecimal("12.0"));
        BigDecimal emi = LoanMath.calculateEmi(new BigDecimal("1000000"), monthlyRate, 60);

        assertEquals(new BigDecimal("22244.45"), emi);
    }

    @Test
    void shouldCalculateReducedTenorWhenEmiKeptFixed() {
        BigDecimal monthlyRate = LoanMath.monthlyRate(new BigDecimal("12.0"));
        int months = LoanMath.calculateTenor(new BigDecimal("480000"), monthlyRate, new BigDecimal("22244.45"));

        assertTrue(months <= 24);
        assertTrue(months >= 21);
    }
}
