package kr.co.seoulit.his.clinical.domain.order.dto;

import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        Long visitId,
        String category,
        String status,
        String primaryItemCode,
        String primaryItemName,
        String idempotencyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // [ADDED] Soft Delete fields
        boolean deleted,
        LocalDateTime deletedAt,
        String deletedBy,
        String deletedReason
) {}
