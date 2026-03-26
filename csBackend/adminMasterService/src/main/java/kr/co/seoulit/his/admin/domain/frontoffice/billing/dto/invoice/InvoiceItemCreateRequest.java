package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record InvoiceItemCreateRequest(
        String itemCode,
        @NotBlank String itemName,
        @NotNull @PositiveOrZero Long unitPrice,
        @NotNull @Positive Integer qty
) {}
