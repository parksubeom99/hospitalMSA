package kr.co.seoulit.his.support.domain.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateInjectionTaskRequest {
    @NotNull
    private Long finalOrderId;

    // note는 선택(작업 지시/메모)
    private String note;

    @NotBlank
    private String idempotencyKey;
}
