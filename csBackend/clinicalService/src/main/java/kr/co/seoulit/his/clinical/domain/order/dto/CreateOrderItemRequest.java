package kr.co.seoulit.his.clinical.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(
        @NotBlank String itemCode,
        @NotBlank String itemName,
        @NotNull @Min(1) Integer qty
) {}
