package kr.co.seoulit.his.admin.domain.master.code.dto;

import jakarta.validation.constraints.NotBlank;

public record CodeSetUpdateRequest(
        @NotBlank String name,
        Boolean active
) {
}
