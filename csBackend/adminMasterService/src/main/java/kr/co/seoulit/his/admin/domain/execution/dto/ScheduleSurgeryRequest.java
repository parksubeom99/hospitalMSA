package kr.co.seoulit.his.admin.domain.execution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * [STEP7] 수술 스케줄 확정(NEW -> IN_PROGRESS)
 */
@Getter @Setter
public class ScheduleSurgeryRequest {
    @NotNull
    private LocalDateTime scheduledAt;

    private String room;
    private String surgeon;
}
