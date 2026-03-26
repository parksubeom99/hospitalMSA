package kr.co.seoulit.his.admin.domain.master.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffUpsertRequest(
        @NotBlank @Size(max = 50) String loginId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String jobType,
        Long departmentId,
        @Size(max = 20) String phone,
        @Size(max = 150) String email,
        Boolean active
) {}
