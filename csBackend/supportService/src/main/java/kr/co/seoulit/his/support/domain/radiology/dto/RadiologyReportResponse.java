package kr.co.seoulit.his.support.domain.radiology.dto;

import java.time.LocalDateTime;

public record RadiologyReportResponse(
        Long reportId,
        Long orderId,
        String reportText,
        String status,
        String idempotencyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy,
        boolean archived,
        LocalDateTime archivedAt,
        String archivedBy,
        String archivedReason
) {}
