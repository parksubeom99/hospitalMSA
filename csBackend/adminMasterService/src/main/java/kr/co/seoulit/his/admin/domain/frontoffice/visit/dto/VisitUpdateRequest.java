package kr.co.seoulit.his.admin.domain.frontoffice.visit.dto;

public record VisitUpdateRequest(
        String patientName,
        String departmentCode,
        String doctorId,
        // [CHANGED] Emergency(B안): 수정/보정 가능 (null이면 변경하지 않음)
        String arrivalType,
        // [CHANGED] Emergency(B안): 수정/보정 가능 (null이면 변경하지 않음)
        Integer triageLevel
) {}
