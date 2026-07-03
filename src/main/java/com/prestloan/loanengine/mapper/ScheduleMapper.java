package com.prestloan.loanengine.mapper;

import com.prestloan.loanengine.dto.ScheduleRowResponse;
import com.prestloan.loanengine.model.LoanSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

    @Mapping(target = "installmentNumber", source = "installmentNumber")
    ScheduleRowResponse toResponse(LoanSchedule schedule);

    List<ScheduleRowResponse> toResponses(List<LoanSchedule> schedules);
}
