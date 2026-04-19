package kr.co.seoulit.his.admin.domain.frontoffice.visit.dto;

import java.time.LocalDateTime;

public record VisitResponse(
        Long visitId,
        Long patientId,
        String patientName,
        // [ADDED v3.3] patient JOIN — 대기 목록에 성별 표시 + 수정 폼 populate 지원
        String gender,
        String rrnMasked,
        String patientPhone,
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
