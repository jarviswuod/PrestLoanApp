package com.prestloan.loanengine.controller;

import com.prestloan.loanengine.dto.*;
import com.prestloan.loanengine.model.LoanStatus;
import com.prestloan.loanengine.model.ScheduleStatus;
import com.prestloan.loanengine.service.LoanService;
import com.prestloan.loanengine.service.PrepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loan Engine", description = "Category A loan prepayment APIs")
@Validated
@RequiredArgsConstructor
@Slf4j
public class LoanController {

    private final LoanService loanService;
    private final PrepaymentService prepaymentService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create loan",
            description = "Creates a loan and generates the initial amortization schedule")
    public LoanResponse createLoan(@Valid @RequestBody CreateLoanRequest request) {
        log.info("Create loan request received");
        LoanResponse response = loanService.createLoan(request);
        log.info("Loan created: loanId={}", response.id());
        return response;
    }


    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan", description = "Fetches a loan by id")
    public LoanResponse getLoan(
            @PathVariable
            @Positive(message = "loanId must be a positive number")
            Long loanId
    ) {
        log.debug("Get loan request received: loanId={}", loanId);
        return loanService.getLoan(loanId);
    }


    @GetMapping
    @Operation(
            summary = "List loans",
            description = "Returns paginated loans with optional status and date-range filters")
    public PagedResponse<LoanResponse> listLoans(
            @RequestParam(required = false)
            LoanStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDateTo,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be greater than or equal to 0")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 200, message = "size cannot exceed 200")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "DESC")
            String sortDir
    ) {
        log.debug("List loans request received: status={}, page={}, size={}", status, page, size);
        return loanService.getLoans(status, startDateFrom, startDateTo, page, size, sortBy, sortDir);
    }


    @GetMapping("/{loanId}/schedule")
    @Operation(
            summary = "Get loan schedule",
            description = "Returns the current schedule state after adjustments")
    public List<ScheduleRowResponse> schedule(
            @PathVariable
            @Positive(message = "loanId must be a positive number")
            Long loanId
    ) {
        log.debug("Get schedule request received: loanId={}", loanId);
        return loanService.getSchedule(loanId);
    }


    @GetMapping("/{loanId}/schedule/search")
    @Operation(
            summary = "Get paged loan schedule",
            description =
                    "Returns paginated schedule rows with optional status and installment-range filters")
    public PagedResponse<ScheduleRowResponse> schedulePaged(
            @PathVariable
            @Positive(message = "loanId must be a positive number")
            Long loanId,

            @RequestParam(required = false)
            ScheduleStatus status,

            @RequestParam(required = false)
            @Min(value = 1, message = "fromInstallment must be at least 1")
            Integer fromInstallment,

            @RequestParam(required = false)
            @Min(value = 1, message = "toInstallment must be at least 1")
            Integer toInstallment,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be greater than or equal to 0")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 200, message = "size cannot exceed 200")
            int size
    ) {
        log.debug(
                "Get paged schedule request received: loanId={}, page={}, size={}, status={}",
                loanId,
                page,
                size,
                status);
        return loanService.getSchedulePage(loanId, status, fromInstallment, toInstallment, page, size);
    }


    @PostMapping("/{loanId}/prepayments")
    @Operation(
            summary = "Apply Category A prepayment",
            description = "Processes prepayment with one of the Category A strategies")
    public PrepaymentResponse prepay(
            @PathVariable
            @Positive(message = "loanId must be a positive number")
            Long loanId,

            @Valid
            @RequestBody
            PrepaymentRequest request
    ) {
        log.info(
                "Prepayment request received: loanId={}, installment={}, option={}",
                loanId,
                request.installmentNumber(),
                request.option());
        return prepaymentService.apply(loanId, request);
    }
}
