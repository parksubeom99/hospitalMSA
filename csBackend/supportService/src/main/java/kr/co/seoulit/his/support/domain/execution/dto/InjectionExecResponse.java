package kr.co.seoulit.his.support.domain.execution.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class InjectionExecResponse {
    private Long injectionExecId;
    private Long finalOrderId;
    private String status;
    private String note;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
