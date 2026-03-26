package kr.co.seoulit.his.admin.domain.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * [STEP7] Finalize 이후 수술 실행 작업 생성(NEW)
 */
@Getter @Setter
public class CreateSurgeryTaskRequest {
    @NotNull
    private Long finalOrderId;

    @NotBlank
    private String surgeryName;

    private String room; // optional

    @NotBlank
    private String idempotencyKey;
}
