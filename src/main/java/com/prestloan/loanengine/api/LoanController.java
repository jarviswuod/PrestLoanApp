package com.prestloan.loanengine.api;

import com.prestloan.loanengine.api.dto.CreateLoanRequest;
import com.prestloan.loanengine.api.dto.LoanResponse;
import com.prestloan.loanengine.api.dto.PrepaymentRequest;
import com.prestloan.loanengine.api.dto.PrepaymentResponse;
import com.prestloan.loanengine.api.dto.ScheduleRowResponse;
import com.prestloan.loanengine.service.LoanService;
import com.prestloan.loanengine.service.PrepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loan Engine", description = "Category A loan prepayment APIs")
public class LoanController {

    private final LoanService loanService;
    private final PrepaymentService prepaymentService;

    public LoanController(LoanService loanService, PrepaymentService prepaymentService) {
        this.loanService = loanService;
        this.prepaymentService = prepaymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create loan", description = "Creates a loan and generates the initial amortization schedule")
    public LoanResponse createLoan(@Valid @RequestBody CreateLoanRequest request) {
        return loanService.createLoan(request);
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan", description = "Fetches a loan by id")
    public LoanResponse getLoan(@PathVariable Long loanId) {
        return loanService.getLoan(loanId);
    }

    @GetMapping("/{loanId}/schedule")
    @Operation(summary = "Get loan schedule", description = "Returns the current schedule state after adjustments")
    public List<ScheduleRowResponse> schedule(@PathVariable Long loanId) {
        return loanService.getSchedule(loanId);
    }

    @PostMapping("/{loanId}/prepayments")
    @Operation(summary = "Apply Category A prepayment", description = "Processes prepayment with one of the Category A strategies")
    public PrepaymentResponse prepay(@PathVariable Long loanId, @Valid @RequestBody PrepaymentRequest request) {
        return prepaymentService.apply(loanId, request);
    }
}
