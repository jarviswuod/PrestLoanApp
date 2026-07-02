package com.prestloan.loanengine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class LoanMath {

    private static final MathContext MC = new MathContext(18, RoundingMode.HALF_UP);

    private LoanMath() {
    }

    public static BigDecimal monthlyRate(BigDecimal annualRatePct) {
        return annualRatePct
                .divide(BigDecimal.valueOf(100), MC)
                .divide(BigDecimal.valueOf(12), MC);
    }

    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal monthlyRate, int tenureMonths) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        double r = monthlyRate.doubleValue();
        double n = tenureMonths;
        double p = principal.doubleValue();
        double pow = Math.pow(1 + r, n);
        double emi = p * r * pow / (pow - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    public static int calculateTenor(BigDecimal principal, BigDecimal monthlyRate, BigDecimal emi) {
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
