package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCreateRequest(
        @NotNull Long invoiceId,
        @NotBlank String method,
        @NotNull @Positive Long amount,
        String idempotencyKey
) {}
