package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice;

import java.util.List;

public record InvoiceHistoryResponse(
        InvoiceResponse invoice,
        List<InvoiceAdjustmentResponse> adjustments
) {}
