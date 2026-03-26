package kr.co.seoulit.his.admin.domain.master.staff.dto;

public record StaffResponse(
        Long staffProfileId,
        String loginId,
        String name,
        String jobType,
        Long departmentId,
        String phone,
        String email,
        boolean active
) {}
