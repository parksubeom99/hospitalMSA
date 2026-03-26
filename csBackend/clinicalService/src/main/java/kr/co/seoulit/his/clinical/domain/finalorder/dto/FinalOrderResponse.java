package kr.co.seoulit.his.clinical.domain.finalorder.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FinalOrderResponse {
    private Long finalOrderId;
    private Long visitId;
    private String type;
    private String status;
    private String note;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
