package kr.co.seoulit.his.admin.domain.master.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ScheduleTemplateCreateRequest(
        @NotNull Long staffProfileId,
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Integer slotMinutes,
        String note
) {
}
