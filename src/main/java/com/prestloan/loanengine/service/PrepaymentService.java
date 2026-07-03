package com.prestloan.loanengine.service;

import com.prestloan.loanengine.dto.PrepaymentRequest;
import com.prestloan.loanengine.dto.PrepaymentResponse;
import com.prestloan.loanengine.exception.BadRequestException;
import com.prestloan.loanengine.exception.NotFoundException;
import com.prestloan.loanengine.model.*;
import com.prestloan.loanengine.repository.LoanRepository;
import com.prestloan.loanengine.repository.LoanScheduleRepository;
import com.prestloan.loanengine.repository.LoanTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrepaymentService {

    private final LoanRepository loanRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final List<PrepaymentComputation> computations;


    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "loanResponses", key = "#loanId"),
                    @CacheEvict(value = "loanSchedules", key = "#loanId"),
                    @CacheEvict(value = "loanListResponses", allEntries = true),
                    @CacheEvict(value = "loanSchedulePages", allEntries = true)
            })
    public PrepaymentResponse apply(Long loanId, PrepaymentRequest request) {
        log.info(
                "Applying prepayment: loanId={}, installment={}, option={}",
                loanId,
                request.installmentNumber(),
                request.option());

        Loan loan =
                loanRepository
                        .findByIdForUpdate(loanId)
                        .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BadRequestException("Loan is already closed");
        }

        validateInstallment(request.installmentNumber(), loan.getTenureMonths());
        validateAmount(request.amount());

        List<LoanSchedule> schedules =
                loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        if (schedules.isEmpty()) {
            throw new BadRequestException("Loan schedule is missing; cannot process prepayment");
        }

        validateOptionSpecificRules(loan, schedules, request);
        PrepaymentSnapshot snapshot = snapshotFor(loan, schedules, request.installmentNumber());

        Set<Long> existingIds =
                schedules.stream().map(LoanSchedule::getId).collect(java.util.stream.Collectors.toSet());
        markPaidThroughInstallment(schedules, request.installmentNumber());

        BigDecimal outstandingBefore = snapshot.outstandingPrincipal();
        if (request.option() != com.prestloan.loanengine.model.PrepaymentOption.ADVANCE_INSTALLMENTS
                && request.amount().compareTo(outstandingBefore) > 0) {
            throw new BadRequestException("Prepayment amount exceeds outstanding principal");
        }

        PrepaymentComputation computation =
                computations.stream()
                        .filter(c -> c.supports(request))
                        .findFirst()
                        .orElseGet(NoopPrepaymentComputation::new);

        PrepaymentResponse response = computation.apply(loan, snapshot, schedules, request);

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
            log.debug("Removed superseded schedule rows: loanId={}, rows={}", loanId, removedIds.size());
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

        log.info(
                "Prepayment applied: loanId={}, option={}, remainingTenorMonths={}, loanStatus={}",
                loanId,
                request.option(),
                response.remainingTenorMonths(),
                loan.getStatus());

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
        if (amount.scale() > 2) {
            throw new BadRequestException("Prepayment amount cannot have more than 2 decimal places");
        }
    }


    private void validateOptionSpecificRules(
            Loan loan,
            List<LoanSchedule> schedules,
            PrepaymentRequest request
    ) {
        boolean hasFutureInstallments =
                schedules.stream().anyMatch(s -> s.getInstallmentNumber() > request.installmentNumber());

        if (request.option() == PrepaymentOption.ADVANCE_INSTALLMENTS && !hasFutureInstallments) {
            throw new BadRequestException(
                    "Advance installments option requires at least one future installment");
        }

        if ((request.option() == PrepaymentOption.REDUCE_EMI_KEEP_TENOR
                || request.option() == PrepaymentOption.REDUCE_TENOR_KEEP_EMI)
                && request.installmentNumber() >= loan.getTenureMonths()) {
            throw new BadRequestException(
                    "Recalculation options require remaining tenure after selected installment");
        }
    }


    private void markPaidThroughInstallment(List<LoanSchedule> schedules, int installmentNumber) {
        schedules.stream()
                .filter(s -> s.getInstallmentNumber() <= installmentNumber)
                .forEach(s -> s.setStatus(ScheduleStatus.PAID));
    }


    private PrepaymentSnapshot snapshotFor(
            Loan loan,
            List<LoanSchedule> schedules,
            int installmentNumber
    ) {
        LoanSchedule paidRow =
                schedules.stream()
                        .filter(s -> s.getInstallmentNumber() == installmentNumber)
                        .findFirst()
                        .orElseThrow(
                                () -> new BadRequestException("Paid-through installment number not found"));

        LocalDate nextDueDate =
                schedules.stream()
                        .filter(s -> s.getInstallmentNumber() == installmentNumber + 1)
                        .map(LoanSchedule::getDueDate)
                        .findFirst()
                        .orElse(loan.getStartDate().plusMonths(installmentNumber));

        return PrepaymentSnapshot.builder()
                .paidThroughInstallmentNumber(installmentNumber)
                .outstandingPrincipal(paidRow.getClosingBalance())
                .remainingTermMonths(loan.getTenureMonths() - installmentNumber)
                .nextDueDate(nextDueDate)
                .build();
    }
}
