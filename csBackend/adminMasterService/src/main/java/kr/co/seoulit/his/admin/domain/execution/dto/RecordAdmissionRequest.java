package kr.co.seoulit.his.admin.domain.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecordAdmissionRequest {
    @NotNull
    private Long finalOrderId;

    @NotBlank
    private String ward;

    @NotBlank
    private String idempotencyKey;
}
