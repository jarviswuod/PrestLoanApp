package com.prestloan.loanengine.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

  private final LoanRepository loanRepository;
  private final LoanScheduleRepository loanScheduleRepository;
  private final ScheduleGenerator scheduleGenerator;
  private final LoanMapper loanMapper;
  private final ScheduleMapper scheduleMapper;

  @Transactional
  @CacheEvict(
      value = {"loanResponses", "loanSchedules", "loanListResponses", "loanSchedulePages"},
      allEntries = true)
  public LoanResponse createLoan(CreateLoanRequest request) {
    validateCreateLoanBusinessRules(request);
    log.info("Creating loan aggregate: tenureMonths={}", request.tenureMonths());

    Loan loan = loanMapper.toEntity(request);

    BigDecimal initialEmi =
        LoanMath.calculateEmi(
            loan.getPrincipal(),
            LoanMath.monthlyRate(loan.getAnnualInterestRate()),
            loan.getTenureMonths());
    loan.setEmi(initialEmi);
    loan.setOriginalEmi(initialEmi);

    Loan saved = loanRepository.save(loan);

    List<LoanSchedule> schedule =
        scheduleGenerator.generate(
            saved,
            1,
            saved.getTenureMonths(),
            saved.getStartDate(),
            saved.getPrincipal(),
            saved.getEmi(),
            ScheduleStatus.PENDING);
    loanScheduleRepository.saveAll(schedule);
    log.info("Loan aggregate created: loanId={}, scheduleRows={}", saved.getId(), schedule.size());

    return loanMapper.toResponse(saved);
  }

  @Transactional(readOnly = true)
  public Loan getLoanEntity(Long loanId) {
    log.debug("Fetching loan entity: loanId={}", loanId);
    return loanRepository
        .findById(loanId)
        .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "loanResponses", key = "#loanId", sync = true)
  public LoanResponse getLoan(Long loanId) {
    return loanMapper.toResponse(getLoanEntity(loanId));
  }

  @Transactional(readOnly = true)
  @Cacheable(
      value = "loanListResponses",
      key =
          "#status + '|' + #startDateFrom + '|' + #startDateTo + '|' + #page + '|' + #size + '|' + #sortBy + '|' + #sortDir",
      sync = true)
  public PagedResponse<LoanResponse> getLoans(
      LoanStatus status,
      LocalDate startDateFrom,
      LocalDate startDateTo,
      int page,
      int size,
      String sortBy,
      String sortDir) {
    validatePageParams(page, size);
    validateDateRange(startDateFrom, startDateTo);

    Sort sort = buildLoanSort(sortBy, sortDir);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Loan> result =
        loanRepository.searchByFilters(status, startDateFrom, startDateTo, pageable);

    List<LoanResponse> content = result.getContent().stream().map(loanMapper::toResponse).toList();

    return toPagedResponse(content, result);
  }

  @Transactional(readOnly = true)
  @Cacheable(value = "loanSchedules", key = "#loanId", sync = true)
  public List<ScheduleRowResponse> getSchedule(Long loanId) {
    getLoanEntity(loanId);
    List<LoanSchedule> schedules =
        loanScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
    log.debug("Fetched schedule rows: loanId={}, rows={}", loanId, schedules.size());
    return scheduleMapper.toResponses(schedules);
  }

  @Transactional(readOnly = true)
  @Cacheable(
      value = "loanSchedulePages",
      key =
          "#loanId + '|' + #status + '|' + #fromInstallment + '|' + #toInstallment + '|' + #page + '|' + #size",
      sync = true)
  public PagedResponse<ScheduleRowResponse> getSchedulePage(
      Long loanId,
      ScheduleStatus status,
      Integer fromInstallment,
      Integer toInstallment,
      int page,
      int size) {
    getLoanEntity(loanId);
    validatePageParams(page, size);
    validateInstallmentRange(fromInstallment, toInstallment);

    Pageable pageable =
        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "installmentNumber"));
    Page<LoanSchedule> schedules =
        loanScheduleRepository.searchByLoanIdAndFilters(
            loanId, status, fromInstallment, toInstallment, pageable);

    List<ScheduleRowResponse> content = scheduleMapper.toResponses(schedules.getContent());
    log.debug(
        "Fetched paged schedule rows: loanId={}, rows={}, page={}", loanId, content.size(), page);
    return toPagedResponse(content, schedules);
  }

  private Sort buildLoanSort(String sortBy, String sortDir) {
    String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
    if (!List.of("createdAt", "startDate", "id", "status").contains(normalizedSortBy)) {
      throw new IllegalArgumentException(
          "Unsupported sortBy. Allowed values: createdAt, startDate, id, status");
    }

    Sort.Direction direction =
        "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    return Sort.by(direction, normalizedSortBy);
  }

  private void validatePageParams(int page, int size) {
    if (page < 0) {
      throw new IllegalArgumentException("page must be greater than or equal to 0");
    }
    if (size < 1 || size > 200) {
      throw new IllegalArgumentException("size must be between 1 and 200");
    }
  }

  private void validateDateRange(LocalDate startDateFrom, LocalDate startDateTo) {
    if (startDateFrom != null && startDateTo != null && startDateFrom.isAfter(startDateTo)) {
      throw new IllegalArgumentException("startDateFrom cannot be after startDateTo");
    }
  }

  private void validateInstallmentRange(Integer fromInstallment, Integer toInstallment) {
    if (fromInstallment != null && fromInstallment < 1) {
      throw new IllegalArgumentException("fromInstallment must be at least 1");
    }
    if (toInstallment != null && toInstallment < 1) {
      throw new IllegalArgumentException("toInstallment must be at least 1");
    }
    if (fromInstallment != null && toInstallment != null && fromInstallment > toInstallment) {
      throw new IllegalArgumentException("fromInstallment cannot be greater than toInstallment");
    }
  }

  private <T> PagedResponse<T> toPagedResponse(List<T> content, Page<?> page) {
    return new PagedResponse<>(
        content,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }

  private void validateCreateLoanBusinessRules(CreateLoanRequest request) {
    if (request.tenureMonths() > 600) {
      throw new IllegalArgumentException("tenureMonths cannot exceed 600 months");
    }
    if (request.annualInterestRate().compareTo(new BigDecimal("100.00")) > 0) {
      throw new IllegalArgumentException("annualInterestRate cannot exceed 100.00 percent");
    }
  }
}
