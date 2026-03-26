package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice;

import jakarta.validation.constraints.NotNull;

public record InvoiceAdjustRequest(
        @NotNull Integer newAmount,
        String reason
) {}
