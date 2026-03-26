package kr.co.seoulit.his.admin.domain.execution.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AdmissionExecResponse {
    private Long admissionExecId;
    private Long finalOrderId;
    private String ward;
    private String status;
    private String idempotencyKey;
    private LocalDateTime admittedAt; // [ADDED][STEP6]
    private LocalDateTime dischargedAt; // [ADDED][STEP6]
    private LocalDateTime updatedAt; // [ADDED][STEP6]
    private LocalDateTime createdAt;
}
