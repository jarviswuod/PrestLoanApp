package com.prestloan.loanengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.api.error.BadRequestException;
import com.prestloan.loanengine.api.error.NotFoundException;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.LoanStatus;
import com.prestloan.loanengine.domain.LoanTransaction;
import com.prestloan.loanengine.domain.PrepaymentOption;
import com.prestloan.loanengine.domain.ScheduleStatus;
import com.prestloan.loanengine.domain.TransactionType;
import com.prestloan.loanengine.repository.LoanRepository;
import com.prestloan.loanengine.repository.LoanScheduleRepository;
import com.prestloan.loanengine.repository.LoanTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrepaymentServiceTest {

  private PrepaymentService service;

  @Mock private LoanRepository loanRepository;

  @Mock private LoanScheduleRepository loanScheduleRepository;

  @Mock private LoanTransactionRepository loanTransactionRepository;

  @Mock private PrepaymentComputation computation;

  @BeforeEach
  void setUp() {
    service =
        new PrepaymentService(
            loanRepository,
            loanScheduleRepository,
            loanTransactionRepository,
            List.of(computation));
  }

  @Test
  void shouldThrowWhenLoanNotFound() {
    PrepaymentRequest request =
        new PrepaymentRequest(24, new BigDecimal("200000"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR);
    when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.apply(1L, request))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Loan not found: 1");
  }

  @Test
  void shouldThrowWhenLoanIsClosed() {
    Loan loan = new Loan();
    loan.setStatus(LoanStatus.CLOSED);
    when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

    PrepaymentRequest request =
        new PrepaymentRequest(24, new BigDecimal("200000"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR);

    assertThatThrownBy(() -> service.apply(1L, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Loan is already closed");
  }

  @Test
  void shouldThrowWhenInstallmentOutOfRange() {
    Loan loan = activeLoan();
    loan.setTenureMonths(12);
    when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

    PrepaymentRequest request =
        new PrepaymentRequest(13, new BigDecimal("200000"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR);

    assertThatThrownBy(() -> service.apply(1L, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Installment must be between 1 and 12");
  }

  @Test
  void shouldThrowWhenAmountScaleInvalid() {
    Loan loan = activeLoan();
    when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

    PrepaymentRequest request =
        new PrepaymentRequest(1, new BigDecimal("100.123"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR);

    assertThatThrownBy(() -> service.apply(1L, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Prepayment amount cannot have more than 2 decimal places");
  }

  @Test
  void shouldThrowWhenAmountExceedsOutstanding() {
    Loan loan = activeLoan();
    when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

    LoanSchedule paid = schedule(24, new BigDecimal("100.00"), 1L);
    LoanSchedule next = schedule(25, new BigDecimal("90.00"), 2L);
    when(loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(1L))
        .thenReturn(new ArrayList<>(List.of(paid, next)));

    PrepaymentRequest request =
        new PrepaymentRequest(24, new BigDecimal("200.00"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR);

    assertThatThrownBy(() -> service.apply(1L, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Prepayment amount exceeds outstanding principal");
  }

  @Test
  void shouldApplyPrepaymentAndPersistTransaction() {
    Loan loan = activeLoan();
    loan.setId(1L);
    when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

    LoanSchedule paid = schedule(24, new BigDecimal("600000.00"), 1L);
    LoanSchedule next = schedule(25, new BigDecimal("590000.00"), 2L);
    when(loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(1L))
        .thenReturn(new ArrayList<>(List.of(paid, next)));

    when(computation.supports(any(PrepaymentRequest.class))).thenReturn(true);
    when(computation.apply(
            any(Loan.class),
            any(PrepaymentSnapshot.class),
            any(List.class),
            any(PrepaymentRequest.class)))
        .thenReturn(
        PrepaymentResponse.builder()
          .loanId(1L)
          .option(PrepaymentOption.REDUCE_EMI_KEEP_TENOR)
          .installmentNumber(24)
          .prepaymentAmount(new BigDecimal("200000.00"))
          .outstandingBefore(new BigDecimal("600000.00"))
          .outstandingAfter(new BigDecimal("400000.00"))
          .newEmi(new BigDecimal("15000.00"))
          .remainingTenorMonths(36)
          .advancedInstallments(0)
          .notes("updated")
          .build());

    PrepaymentRequest request =
        new PrepaymentRequest(
            24, new BigDecimal("200000.00"), PrepaymentOption.REDUCE_EMI_KEEP_TENOR);
    PrepaymentResponse result = service.apply(1L, request);

    assertThat(result.newEmi()).isEqualByComparingTo("15000.00");
    verify(loanRepository).save(loan);
    assertThat(loan.getEmi()).isEqualByComparingTo("15000.00");

    ArgumentCaptor<LoanTransaction> txCaptor = ArgumentCaptor.forClass(LoanTransaction.class);
    verify(loanTransactionRepository).save(txCaptor.capture());
    assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionType.PREPAYMENT);
    assertThat(txCaptor.getValue().getInstallmentNumber()).isEqualTo(24);
    verify(loanScheduleRepository).saveAll(any(List.class));
  }

  @Test
  void shouldRequireFutureInstallmentsForAdvanceOption() {
    Loan loan = activeLoan();
    loan.setTenureMonths(24);
    when(loanRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(loan));

    LoanSchedule paid = schedule(24, new BigDecimal("0.00"), 11L);
    when(loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(1L))
        .thenReturn(new ArrayList<>(List.of(paid)));

    PrepaymentRequest request =
        new PrepaymentRequest(24, new BigDecimal("1000.00"), PrepaymentOption.ADVANCE_INSTALLMENTS);

    assertThatThrownBy(() -> service.apply(1L, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Advance installments option requires at least one future installment");
    verify(loanRepository, never()).save(any(Loan.class));
  }

  private Loan activeLoan() {
    Loan loan = new Loan();
    loan.setId(1L);
    loan.setStatus(LoanStatus.ACTIVE);
    loan.setTenureMonths(60);
    loan.setEmi(new BigDecimal("22244.45"));
    loan.setAnnualInterestRate(new BigDecimal("12.0"));
    loan.setStartDate(LocalDate.of(2026, 1, 1));
    return loan;
  }

  private LoanSchedule schedule(int installment, BigDecimal closingBalance, Long id) {
    LoanSchedule schedule = new LoanSchedule();
    schedule.setId(id);
    schedule.setInstallmentNumber(installment);
    schedule.setClosingBalance(closingBalance);
    schedule.setEmiAmount(new BigDecimal("22244.45"));
    schedule.setDueDate(LocalDate.of(2026, 1, 1).plusMonths(installment - 1L));
    schedule.setStatus(ScheduleStatus.PENDING);
    return schedule;
  }
}
