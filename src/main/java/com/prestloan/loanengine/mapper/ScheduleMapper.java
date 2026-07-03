package com.prestloan.loanengine.mapper;

import com.prestloan.loanengine.api.dto.ScheduleRowResponse;
import com.prestloan.loanengine.domain.LoanSchedule;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

  @Mapping(target = "installmentNumber", source = "installmentNumber")
  ScheduleRowResponse toResponse(LoanSchedule schedule);

  List<ScheduleRowResponse> toResponses(List<LoanSchedule> schedules);
}
