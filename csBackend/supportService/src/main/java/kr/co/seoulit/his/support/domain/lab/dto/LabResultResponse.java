package kr.co.seoulit.his.support.domain.lab.dto;

import java.time.LocalDateTime;

public record LabResultResponse(
        Long labResultId,
        Long orderId,
        String resultText,
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
