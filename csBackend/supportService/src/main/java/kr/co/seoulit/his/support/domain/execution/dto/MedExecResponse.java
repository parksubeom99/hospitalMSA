package kr.co.seoulit.his.support.domain.execution.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MedExecResponse {
    private Long medExecId;
    private Long finalOrderId;
    private String status; // NEW / IN_PROGRESS / DONE
    private String note;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
