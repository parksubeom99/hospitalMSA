package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice;

import java.time.LocalDateTime;

public record InvoiceAdjustmentResponse(
        Long adjustmentId,
        String type,
        Integer oldAmount,
        Integer newAmount,
        String reason,
        LocalDateTime createdAt
) {}
