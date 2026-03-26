package kr.co.seoulit.his.admin.domain.execution.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SurgeryExecResponse {
    private Long surgeryExecId;
    private Long finalOrderId;
    private String surgeryName;
    private String room;
    private String surgeon;
    private String status;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
}
