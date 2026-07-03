package com.prestloan.loanengine.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loans")
@Builder
@SQLDelete(sql = "UPDATE loans SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id=?")
@SQLRestriction("is_deleted = false")
public class Loan extends BaseEntity {

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(name = "annual_interest_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal annualInterestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "original_tenure_months", nullable = false)
    private Integer originalTenureMonths;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal emi;

    @Column(name = "original_emi", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalEmi;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.ACTIVE;
}
