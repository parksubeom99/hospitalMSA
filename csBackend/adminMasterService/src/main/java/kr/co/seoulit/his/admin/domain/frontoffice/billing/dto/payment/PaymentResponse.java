package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.payment;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long invoiceId,
        String method,
        Long amount,
        String status,
        String idempotencyKey,
        LocalDateTime paidAt
) {}
