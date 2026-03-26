package kr.co.seoulit.his.admin.domain.master.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        Boolean active
) {}
