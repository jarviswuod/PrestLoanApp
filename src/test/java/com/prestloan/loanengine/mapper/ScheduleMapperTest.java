package com.prestloan.loanengine.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.prestloan.loanengine.api.dto.ScheduleRowResponse;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleMapperTest {

  private final ScheduleMapper mapper = new ScheduleMapperImpl();

  @Test
  void shouldMapScheduleEntityListToResponseList() {
    LoanSchedule row = new LoanSchedule();
    row.setInstallmentNumber(1);
    row.setDueDate(LocalDate.of(2026, 1, 1));
    row.setEmiAmount(new BigDecimal("2000"));
    row.setInterestComponent(new BigDecimal("100"));
    row.setPrincipalComponent(new BigDecimal("1900"));
    row.setOpeningBalance(new BigDecimal("100000"));
    row.setClosingBalance(new BigDecimal("98100"));
    row.setStatus(ScheduleStatus.PENDING);

    List<ScheduleRowResponse> result = mapper.toResponses(List.of(row));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).installmentNumber()).isEqualTo(1);
    assertThat(result.get(0).status()).isEqualTo(ScheduleStatus.PENDING);
  }
}
