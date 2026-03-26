package kr.co.seoulit.his.support.domain.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDispenseRequest(
        @NotNull Long orderId,
        @NotBlank String note,
        @NotBlank String idempotencyKey
) {}
