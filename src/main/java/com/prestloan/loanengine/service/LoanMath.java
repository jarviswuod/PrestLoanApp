package com.prestloan.loanengine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class LoanMath {

  private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);

  private LoanMath() {}

  public static BigDecimal monthlyRate(BigDecimal annualRatePct) {
    if (annualRatePct == null) {
      throw new IllegalArgumentException("annualInterestRate is required");
    }
    if (annualRatePct.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("annualInterestRate cannot be negative");
    }
    return annualRatePct.divide(BigDecimal.valueOf(100), MC).divide(BigDecimal.valueOf(12), MC);
  }

  public static BigDecimal calculateEmi(
      BigDecimal principal, BigDecimal monthlyRate, int tenureMonths) {
    if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("principal must be greater than zero");
    }
    if (monthlyRate == null || monthlyRate.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("monthlyRate cannot be negative");
    }
    if (tenureMonths <= 0) {
      throw new IllegalArgumentException("tenureMonths must be greater than zero");
    }

    if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
      return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
    }

    BigDecimal onePlusRatePower = BigDecimal.ONE.add(monthlyRate, MC).pow(tenureMonths, MC);
    BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(onePlusRatePower, MC);
    BigDecimal denominator = onePlusRatePower.subtract(BigDecimal.ONE, MC);
    return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
  }

  public static int calculateTenor(BigDecimal principal, BigDecimal monthlyRate, BigDecimal emi) {
    if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("principal must be greater than zero");
    }
    if (monthlyRate == null || monthlyRate.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("monthlyRate cannot be negative");
    }
    if (emi == null || emi.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("emi must be greater than zero");
    }

    if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
      return principal.divide(emi, 0, RoundingMode.CEILING).intValue();
    }

    double p = principal.doubleValue();
    double r = monthlyRate.doubleValue();
    double e = emi.doubleValue();

    if (e <= p * r) {
      throw new IllegalArgumentException("EMI is too low to amortize the reduced principal");
    }

    double months = -Math.log(1 - (r * p / e)) / Math.log(1 + r);
    return (int) Math.ceil(months);
  }

  public static BigDecimal roundMoney(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }
}
