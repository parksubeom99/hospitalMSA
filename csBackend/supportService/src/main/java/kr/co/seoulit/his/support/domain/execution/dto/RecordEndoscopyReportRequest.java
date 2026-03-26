package kr.co.seoulit.his.support.domain.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecordEndoscopyReportRequest {
    @NotNull
    private Long finalOrderId;

    @NotBlank
    private String reportText;

    @NotBlank
    private String idempotencyKey;
}
