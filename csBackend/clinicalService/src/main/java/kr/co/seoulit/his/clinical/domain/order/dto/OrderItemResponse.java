package kr.co.seoulit.his.clinical.domain.order.dto;

public record OrderItemResponse(
        Long itemId,
        Long orderId,
        String itemCode,
        String itemName,
        Integer qty
) {}
