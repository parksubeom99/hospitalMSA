package kr.co.seoulit.his.clinical.domain.order.mapper;

import kr.co.seoulit.his.clinical.domain.order.OrderHeader;
import kr.co.seoulit.his.clinical.domain.order.OrderItem;
import kr.co.seoulit.his.clinical.domain.order.dto.OrderItemResponse;
import kr.co.seoulit.his.clinical.domain.order.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponse toResponse(OrderHeader entity);
    List<OrderResponse> toResponseList(List<OrderHeader> entities);

    @Mapping(source = "orderItemId", target = "itemId")
    @Mapping(source = "quantity", target = "qty")
    OrderItemResponse toItemResponse(OrderItem entity);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> entities);
}
