package com.prestloan.loanengine.mapper;

import com.prestloan.loanengine.api.dto.CreateLoanRequest;
import com.prestloan.loanengine.api.dto.LoanResponse;
import com.prestloan.loanengine.domain.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(
      target = "principal",
      expression =
          "java(com.prestloan.loanengine.service.LoanMath.roundMoney(request.principal()))")
  @Mapping(
      target = "status",
      expression = "java(com.prestloan.loanengine.domain.LoanStatus.ACTIVE)")
  @Mapping(target = "emi", ignore = true)
  @Mapping(target = "originalEmi", ignore = true)
  @Mapping(target = "originalTenureMonths", source = "tenureMonths")
  Loan toEntity(CreateLoanRequest request);

  LoanResponse toResponse(Loan loan);
}
