package kr.co.seoulit.his.clinical.domain.finalorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateFinalOrderRequest {
    @NotNull
    private Long visitId;

    @NotBlank
    private String type;

    private String note;

    @NotBlank
    private String idempotencyKey;
}
