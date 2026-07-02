package com.prestloan.loanengine.service;

import com.prestloan.loanengine.api.dto.CreateLoanRequest;
import com.prestloan.loanengine.api.dto.LoanResponse;
import com.prestloan.loanengine.api.dto.ScheduleRowResponse;
import com.prestloan.loanengine.api.error.NotFoundException;
import com.prestloan.loanengine.domain.Loan;
import com.prestloan.loanengine.domain.LoanStatus;
import com.prestloan.loanengine.domain.LoanSchedule;
import com.prestloan.loanengine.domain.ScheduleStatus;
import com.prestloan.loanengine.repository.LoanRepository;
import com.prestloan.loanengine.repository.LoanScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanScheduleRepository loanScheduleRepository;
    private final ScheduleGenerator scheduleGenerator;

    public LoanService(LoanRepository loanRepository,
                       LoanScheduleRepository loanScheduleRepository,
                       ScheduleGenerator scheduleGenerator) {
        this.loanRepository = loanRepository;
        this.loanScheduleRepository = loanScheduleRepository;
        this.scheduleGenerator = scheduleGenerator;
    }

    @Transactional
    public LoanResponse createLoan(CreateLoanRequest request) {
        Loan loan = new Loan();
        loan.setPrincipal(LoanMath.roundMoney(request.principal()));
        loan.setAnnualInterestRate(request.annualInterestRate());
        loan.setTenureMonths(request.tenureMonths());
        loan.setStartDate(request.startDate());
        loan.setStatus(LoanStatus.ACTIVE);

        loan.setEmi(LoanMath.calculateEmi(
                loan.getPrincipal(),
                LoanMath.monthlyRate(loan.getAnnualInterestRate()),
                loan.getTenureMonths()
        ));

        Loan saved = loanRepository.save(loan);

        List<LoanSchedule> schedule = scheduleGenerator.generate(
                saved,
                1,
                saved.getTenureMonths(),
                saved.getStartDate(),
                saved.getPrincipal(),
                saved.getEmi(),
                ScheduleStatus.PENDING
        );
        loanScheduleRepository.saveAll(schedule);

        return toLoanResponse(saved);
    }

    @Transactional(readOnly = true)
    public Loan getLoanEntity(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoan(Long loanId) {
        return toLoanResponse(getLoanEntity(loanId));
    }

    @Transactional(readOnly = true)
    public List<ScheduleRowResponse> getSchedule(Long loanId) {
        getLoanEntity(loanId);
        return loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
                .map(this::toScheduleRow)
                .toList();
    }

    private LoanResponse toLoanResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getPrincipal(),
                loan.getAnnualInterestRate(),
                loan.getTenureMonths(),
                loan.getEmi(),
                loan.getStartDate(),
                loan.getStatus()
        );
    }

    private ScheduleRowResponse toScheduleRow(LoanSchedule row) {
        return new ScheduleRowResponse(
                row.getInstallmentNumber(),
                row.getDueDate(),
                row.getOpeningBalance(),
                row.getEmiAmount(),
                row.getPrincipalComponent(),
                row.getInterestComponent(),
                row.getClosingBalance(),
                row.getStatus()
        );
    }
}
