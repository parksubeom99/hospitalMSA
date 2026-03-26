package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice;

import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long invoiceId,
        Long visitId,
        String status,
        Long totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<InvoiceItemResponse> items
) {}
