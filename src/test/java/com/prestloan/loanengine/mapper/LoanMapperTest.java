package com.prestloan.loanengine.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.prestloan.loanengine.api.dto.CreateLoanRequest;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LoanMapperTest {

  private final LoanMapper mapper = new LoanMapperImpl();

  @Test
  void shouldMapCreateLoanRequestToEntityWithDefaults() {
    CreateLoanRequest request =
        new CreateLoanRequest(
            new BigDecimal("1000000.129"), new BigDecimal("12.0"), 60, LocalDate.of(2026, 1, 1));

    Loan mapped = mapper.toEntity(request);

    assertThat(mapped.getPrincipal()).isEqualByComparingTo("1000000.13");
    assertThat(mapped.getOriginalTenureMonths()).isEqualTo(60);
    assertThat(mapped.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    assertThat(mapped.getEmi()).isNull();
  }
}
