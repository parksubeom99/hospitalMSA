package kr.co.seoulit.his.admin.domain.master.code.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeSetCreateRequest(
        @NotBlank @Size(max = 50) String codeSetKey,
        @NotBlank @Size(max = 100) String name
) {}
