package kr.co.seoulit.his.clinical.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull Long visitId,
        @NotBlank String category,
        @Valid List<CreateOrderItemRequest> items,
        @NotBlank String idempotencyKey
) {}
