package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.receipt;

import java.time.LocalDateTime;

public record ReceiptResponse(
        Long receiptId,
        Long paymentId,
        String receiptNo,
        LocalDateTime createdAt
) {}
