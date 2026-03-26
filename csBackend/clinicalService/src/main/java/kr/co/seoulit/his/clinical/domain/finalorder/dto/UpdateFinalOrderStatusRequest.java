package kr.co.seoulit.his.clinical.domain.finalorder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateFinalOrderStatusRequest {
    @NotBlank
    private String status;
}
