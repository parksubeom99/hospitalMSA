package kr.co.seoulit.his.admin.domain.master.department.dto;

public record DepartmentResponse(
        Long departmentId,
        String code,
        String name,
        boolean active
) {}
