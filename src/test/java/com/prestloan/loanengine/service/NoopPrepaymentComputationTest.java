package com.prestloan.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.PrepaymentOption;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class NoopPrepaymentComputationTest {

  @Test
  void shouldNeverSupportAndThrowOnApply() {
    NoopPrepaymentComputation computation = new NoopPrepaymentComputation();
    PrepaymentRequest request =
        new PrepaymentRequest(1, new BigDecimal("10.00"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR);

    assertThat(computation.supports(request)).isFalse();
    assertThatThrownBy(
            () ->
                computation.apply(
                    new Loan(),
                    new PrepaymentSnapshot(1, BigDecimal.ONE, 1, java.time.LocalDate.now()),
                    List.of(),
                    request))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Unsupported prepayment strategy");
  }
}
