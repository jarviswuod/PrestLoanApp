package com.prestloan.loanengine.service;

import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.api.error.BadRequestException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PrepaymentService {

    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final List<PrepaymentComputation> computations;

    public PrepaymentService(LoanService loanService,
                             LoanRepository loanRepository,
                             LoanScheduleRepository loanScheduleRepository,
                             LoanTransactionRepository loanTransactionRepository,
                             List<PrepaymentComputation> computations) {
        this.loanService = loanService;
        this.loanRepository = loanRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.loanTransactionRepository = loanTransactionRepository;
        this.computations = computations;
    }

    @Transactional
    public PrepaymentResponse apply(Long loanId, PrepaymentRequest request) {
        Loan loan = loanService.getLoanEntity(loanId);
        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BadRequestException("Loan is already closed");
        }

        validateInstallment(request.installmentNumber(), loan.getTenureMonths());
        validateAmount(request.amount());

        List<LoanSchedule> schedules = loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        Set<Long> existingIds = schedules.stream()
            .map(LoanSchedule::getId)
            .collect(java.util.stream.Collectors.toSet());
        markPaidUntilInstallment(schedules, request.installmentNumber());

        LoanSchedule selected = schedules.stream()
                .filter(s -> s.getInstallmentNumber() == request.installmentNumber())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Installment number not found"));

        BigDecimal outstandingBefore = selected.getClosingBalance();
        if (request.option() != com.prestloan.loanengine.domain.PrepaymentOption.ADVANCE_INSTALLMENTS
                && request.amount().compareTo(outstandingBefore) > 0) {
            throw new BadRequestException("Prepayment amount exceeds outstanding principal");
        }

        PrepaymentComputation computation = computations.stream()
                .filter(c -> c.supports(request))
                .findFirst()
                .orElseGet(NoopPrepaymentComputation::new);

        PrepaymentResponse response = computation.apply(loan, schedules, request);

        Set<Long> remainingIds = new HashSet<>();
        for (LoanSchedule schedule : schedules) {
            if (schedule.getId() != null) {
                remainingIds.add(schedule.getId());
            }
        }

        Set<Long> removedIds = new HashSet<>(existingIds);
        removedIds.removeAll(remainingIds);
        if (!removedIds.isEmpty()) {
            loanScheduleRepository.deleteAllByIdInBatch(removedIds);
        }

        schedules.sort(Comparator.comparingInt(LoanSchedule::getInstallmentNumber));
        loanScheduleRepository.saveAll(schedules);

        if (request.option() == PrepaymentOption.REDUCE_EMI_KEEP_TENOR) {
            loan.setEmi(response.newEmi());
        }
        if (request.option() == PrepaymentOption.REDUCE_TENOR_KEEP_EMI) {
            loan.setTenureMonths(request.installmentNumber() + response.remainingTenorMonths());
        }

        if (response.outstandingAfter().compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }
        loanRepository.save(loan);

        LoanTransaction tx = new LoanTransaction();
        tx.setLoan(loan);
        tx.setType(TransactionType.PREPAYMENT);
        tx.setPrepaymentOption(request.option());
        tx.setAmount(LoanMath.roundMoney(request.amount()));
        tx.setInstallmentNumber(request.installmentNumber());
        tx.setNotes(response.notes());
        loanTransactionRepository.save(tx);

        return response;
    }

    private void validateInstallment(int installment, int tenureMonths) {
        if (installment < 1 || installment > tenureMonths) {
            throw new BadRequestException("Installment must be between 1 and " + tenureMonths);
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Prepayment amount must be positive");
        }
    }

    private void markPaidUntilInstallment(List<LoanSchedule> schedules, int installmentNumber) {
        schedules.stream()
                .filter(s -> s.getInstallmentNumber() <= installmentNumber)
                .forEach(s -> s.setStatus(ScheduleStatus.PAID));
    }
}
