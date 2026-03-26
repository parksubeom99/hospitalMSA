package kr.co.seoulit.his.admin.domain.frontoffice.visit.dto;

import java.time.LocalDateTime;

public record VisitResponse(
        Long visitId,
        Long patientId,
        String patientName,
        String departmentCode,
        String doctorId,
        String status,
        // [CHANGED] Emergency(B안)
        String arrivalType,
        // [CHANGED] Emergency(B안)
        Integer triageLevel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime canceledAt,
        String cancelReason,
        LocalDateTime closedAt
) {}
