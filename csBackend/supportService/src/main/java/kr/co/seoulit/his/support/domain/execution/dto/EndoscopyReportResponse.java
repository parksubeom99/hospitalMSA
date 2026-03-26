package kr.co.seoulit.his.support.domain.execution.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EndoscopyReportResponse {
    private Long endoscopyReportId;
    private Long finalOrderId;
    private String status;
    private String reportText;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
