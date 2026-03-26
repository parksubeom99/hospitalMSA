package kr.co.seoulit.his.admin.domain.master.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

public record ScheduleTemplateUpdateRequest(
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotMinutes,
        Boolean active,
        String note
) {
}
