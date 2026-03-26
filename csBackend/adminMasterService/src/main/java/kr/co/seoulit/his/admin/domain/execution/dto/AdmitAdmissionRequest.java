package kr.co.seoulit.his.admin.domain.execution.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdmitAdmissionRequest {
    @NotBlank
    private String ward;
}
