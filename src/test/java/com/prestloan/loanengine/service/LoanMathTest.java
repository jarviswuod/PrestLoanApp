package com.prestloan.loanengine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

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
    int months =
        LoanMath.calculateTenor(new BigDecimal("480000"), monthlyRate, new BigDecimal("22244.45"));

    assertTrue(months > 0);
    assertTrue(months < 36);
  }

  @Test
  void shouldRejectNegativeAnnualRate() {
    assertThrows(IllegalArgumentException.class, () -> LoanMath.monthlyRate(new BigDecimal("-1")));
  }

  @Test
  void shouldRejectInvalidEmiInputs() {
    BigDecimal monthlyRate = LoanMath.monthlyRate(new BigDecimal("12.0"));
    assertThrows(
        IllegalArgumentException.class,
        () -> LoanMath.calculateEmi(BigDecimal.ZERO, monthlyRate, 60));
    assertThrows(
        IllegalArgumentException.class,
        () -> LoanMath.calculateEmi(new BigDecimal("1000000"), monthlyRate, 0));
  }

  @Test
  void shouldRejectInvalidTenorInputs() {
    BigDecimal monthlyRate = LoanMath.monthlyRate(new BigDecimal("12.0"));
    assertThrows(
        IllegalArgumentException.class,
        () -> LoanMath.calculateTenor(new BigDecimal("1000"), monthlyRate, BigDecimal.ZERO));
  }

  @Test
  void shouldSupportZeroRateCalculationsAndRounding() {
    BigDecimal emi = LoanMath.calculateEmi(new BigDecimal("12000"), BigDecimal.ZERO, 12);
    int tenor =
        LoanMath.calculateTenor(new BigDecimal("12000"), BigDecimal.ZERO, new BigDecimal("1000"));

    assertEquals(new BigDecimal("1000.00"), emi);
    assertEquals(12, tenor);
    assertEquals(new BigDecimal("10.13"), LoanMath.roundMoney(new BigDecimal("10.125")));
  }

  @Test
  void shouldRejectNonAmortizingEmi() {
    BigDecimal monthlyRate = LoanMath.monthlyRate(new BigDecimal("12.0"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            LoanMath.calculateTenor(new BigDecimal("100000"), monthlyRate, new BigDecimal("500")));
  }
}
