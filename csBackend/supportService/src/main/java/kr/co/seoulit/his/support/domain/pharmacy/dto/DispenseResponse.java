package kr.co.seoulit.his.support.domain.pharmacy.dto;

import java.time.LocalDateTime;

public record DispenseResponse(
        Long dispenseId,
        Long orderId,
        String note,
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
