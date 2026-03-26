package kr.co.seoulit.his.admin.domain.master.code.dto;

import jakarta.validation.constraints.NotBlank;

public record CodeItemUpdateRequest(
        @NotBlank String name,
        Boolean active,
        Integer sortOrder
) {
}
