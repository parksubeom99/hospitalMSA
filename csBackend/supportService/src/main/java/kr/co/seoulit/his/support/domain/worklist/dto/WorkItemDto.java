package kr.co.seoulit.his.support.domain.worklist.dto;

/**
 * Worklist 화면 전용 DTO
 * - Order 서비스의 DTO를 그대로 노출하지 않고, Worklist 전용 DTO로 반환합니다.
 */
public record WorkItemDto(
        Long orderId,
        Long visitId,
        String category,
        String status,
        String primaryItemCode,
        String primaryItemName
) {}
