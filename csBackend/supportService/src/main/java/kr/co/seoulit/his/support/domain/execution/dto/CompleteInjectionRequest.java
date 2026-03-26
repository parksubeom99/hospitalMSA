package kr.co.seoulit.his.support.domain.execution.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CompleteInjectionRequest {
    @NotBlank
    private String note;
}
