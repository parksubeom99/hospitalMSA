package kr.co.seoulit.his.admin.domain.frontoffice.visit.dto;

import jakarta.validation.constraints.NotNull;

public record VisitCreateRequest(
        @NotNull Long patientId,
        String patientName,
        String departmentCode,
        String doctorId,
        // [CHANGED] Emergency(B안): 비응급이면 null 가능
        String arrivalType,
        // [CHANGED] Emergency(B안): 비응급이면 null 가능
        Integer triageLevel
) {}
