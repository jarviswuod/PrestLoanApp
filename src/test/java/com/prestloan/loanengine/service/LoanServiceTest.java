package com.prestloan.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prestloan.loanengine.api.dto.CreateLoanRequest;
import com.prestloan.loanengine.api.dto.LoanResponse;
import com.prestloan.loanengine.api.dto.PagedResponse;
import com.prestloan.loanengine.api.dto.ScheduleRowResponse;
import com.prestloan.loanengine.api.error.NotFoundException;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.LoanStatus;
import com.prestloan.loanengine.domain.ScheduleStatus;
import com.prestloan.loanengine.mapper.LoanMapper;
import com.prestloan.loanengine.mapper.ScheduleMapper;
import com.prestloan.loanengine.repository.LoanRepository;
import com.prestloan.loanengine.repository.LoanScheduleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

  @InjectMocks private LoanService loanService;

  @Mock private LoanRepository loanRepository;

  @Mock private LoanScheduleRepository loanScheduleRepository;

  @Mock private ScheduleGenerator scheduleGenerator;

  @Mock private LoanMapper loanMapper;

  @Mock private ScheduleMapper scheduleMapper;

  @Test
  void shouldCreateLoanAndPersistSchedule() {
    CreateLoanRequest request =
        new CreateLoanRequest(
            new BigDecimal("1000000"), new BigDecimal("12.0"), 60, LocalDate.of(2026, 1, 1));

    Loan mappedLoan = new Loan();
    mappedLoan.setPrincipal(new BigDecimal("1000000"));
    mappedLoan.setAnnualInterestRate(new BigDecimal("12.0"));
    mappedLoan.setTenureMonths(60);
    mappedLoan.setStartDate(LocalDate.of(2026, 1, 1));

    Loan savedLoan = new Loan();
    savedLoan.setId(10L);
    savedLoan.setPrincipal(new BigDecimal("1000000"));
    savedLoan.setAnnualInterestRate(new BigDecimal("12.0"));
    savedLoan.setTenureMonths(60);
    savedLoan.setStartDate(LocalDate.of(2026, 1, 1));

    LoanSchedule scheduleRow = new LoanSchedule();
    scheduleRow.setInstallmentNumber(1);

    LoanResponse expected =
        LoanResponse.builder()
            .id(10L)
            .principal(new BigDecimal("1000000.00"))
            .annualInterestRate(new BigDecimal("12.0"))
            .originalTenureMonths(60)
            .tenureMonths(60)
            .originalEmi(new BigDecimal("22244.45"))
            .emi(new BigDecimal("22244.45"))
            .startDate(LocalDate.of(2026, 1, 1))
            .status(LoanStatus.ACTIVE)
            .build();

    when(loanMapper.toEntity(request)).thenReturn(mappedLoan);
    when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);
    when(scheduleGenerator.generate(
            any(Loan.class),
            eq(1),
            eq(60),
            eq(LocalDate.of(2026, 1, 1)),
            eq(new BigDecimal("1000000")),
            any(),
            eq(ScheduleStatus.PENDING)))
        .thenReturn(List.of(scheduleRow));
    when(loanMapper.toResponse(savedLoan)).thenReturn(expected);

    LoanResponse actual = loanService.createLoan(request);

    assertThat(actual.id()).isEqualTo(10L);
    ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
    verify(loanRepository).save(loanCaptor.capture());
    assertThat(loanCaptor.getValue().getEmi()).isNotNull();
    assertThat(loanCaptor.getValue().getOriginalEmi()).isEqualTo(loanCaptor.getValue().getEmi());
    verify(loanScheduleRepository).saveAll(List.of(scheduleRow));
  }

  @Test
  void shouldGetLoanById() {
    Loan loan = new Loan();
    loan.setId(1L);

    LoanResponse response =
        LoanResponse.builder()
            .id(1L)
            .principal(new BigDecimal("1000000.00"))
            .annualInterestRate(new BigDecimal("12.0"))
            .originalTenureMonths(60)
            .tenureMonths(60)
            .originalEmi(new BigDecimal("22244.45"))
            .emi(new BigDecimal("22244.45"))
            .startDate(LocalDate.of(2026, 1, 1))
            .status(LoanStatus.ACTIVE)
            .build();

    when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
    when(loanMapper.toResponse(loan)).thenReturn(response);

    LoanResponse actual = loanService.getLoan(1L);

    assertThat(actual.id()).isEqualTo(1L);
  }

  @Test
  void shouldThrowWhenLoanNotFound() {
    when(loanRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> loanService.getLoanEntity(99L))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Loan not found: 99");
  }

  @Test
  void shouldReturnPagedLoanListWithFilters() {
    Loan loan = new Loan();
    loan.setId(2L);
    loan.setStatus(LoanStatus.ACTIVE);

    LoanResponse mapped =
        LoanResponse.builder()
            .id(2L)
            .principal(new BigDecimal("1000000.00"))
            .annualInterestRate(new BigDecimal("12.0"))
            .originalTenureMonths(60)
            .tenureMonths(60)
            .originalEmi(new BigDecimal("22244.45"))
            .emi(new BigDecimal("22244.45"))
            .startDate(LocalDate.of(2026, 1, 1))
            .status(LoanStatus.ACTIVE)
            .build();

    Page<Loan> page = new PageImpl<>(List.of(loan), Pageable.ofSize(10), 1);
    when(loanRepository.searchByFilters(
            eq(LoanStatus.ACTIVE),
            eq(LocalDate.of(2026, 1, 1)),
            eq(LocalDate.of(2026, 12, 31)),
            any(Pageable.class)))
        .thenReturn(page);
    when(loanMapper.toResponse(loan)).thenReturn(mapped);

    PagedResponse<LoanResponse> result =
        loanService.getLoans(
            LoanStatus.ACTIVE,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            0,
            10,
            "createdAt",
            "DESC");

    assertThat(result.totalElements()).isEqualTo(1);
    assertThat(result.content()).hasSize(1);
  }

  @Test
  void shouldValidatePageAndRangeInputs() {
    assertThatThrownBy(
            () -> loanService.getLoans(LoanStatus.ACTIVE, null, null, -1, 10, "createdAt", "DESC"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("page must be greater than or equal to 0");

    assertThatThrownBy(
            () -> loanService.getLoans(LoanStatus.ACTIVE, null, null, 0, 500, "createdAt", "DESC"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("size must be between 1 and 200");

    assertThatThrownBy(
            () ->
                loanService.getLoans(
                    LoanStatus.ACTIVE,
                    LocalDate.of(2026, 2, 1),
                    LocalDate.of(2026, 1, 1),
                    0,
                    20,
                    "createdAt",
                    "DESC"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("startDateFrom cannot be after startDateTo");

    assertThatThrownBy(
            () -> loanService.getLoans(LoanStatus.ACTIVE, null, null, 0, 20, "invalid", "DESC"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported sortBy");
  }

  @Test
  void shouldReturnPagedSchedule() {
    Loan loan = new Loan();
    loan.setId(3L);

    LoanSchedule row = new LoanSchedule();
    row.setInstallmentNumber(10);

    ScheduleRowResponse mapped =
        ScheduleRowResponse.builder()
            .installmentNumber(10)
            .dueDate(LocalDate.of(2026, 10, 1))
            .openingBalance(new BigDecimal("100.00"))
            .emiAmount(new BigDecimal("10.00"))
            .principalComponent(new BigDecimal("7.00"))
            .interestComponent(new BigDecimal("3.00"))
            .closingBalance(new BigDecimal("93.00"))
            .status(ScheduleStatus.PENDING)
            .build();

    Page<LoanSchedule> page = new PageImpl<>(List.of(row), Pageable.ofSize(5), 1);

    when(loanRepository.findById(3L)).thenReturn(Optional.of(loan));
    when(loanScheduleRepository.searchByLoanIdAndFilters(
            eq(3L), eq(ScheduleStatus.PENDING), eq(10), eq(20), any(Pageable.class)))
        .thenReturn(page);
    when(scheduleMapper.toResponses(List.of(row))).thenReturn(List.of(mapped));

    PagedResponse<ScheduleRowResponse> result =
        loanService.getSchedulePage(3L, ScheduleStatus.PENDING, 10, 20, 0, 5);

    assertThat(result.totalElements()).isEqualTo(1);
    assertThat(result.content()).hasSize(1);
  }

  @Test
  void shouldValidateInstallmentRange() {
    Loan loan = new Loan();
    loan.setId(8L);
    when(loanRepository.findById(8L)).thenReturn(Optional.of(loan));

    assertThatThrownBy(() -> loanService.getSchedulePage(8L, null, 20, 10, 0, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("fromInstallment cannot be greater than toInstallment");
  }
}
