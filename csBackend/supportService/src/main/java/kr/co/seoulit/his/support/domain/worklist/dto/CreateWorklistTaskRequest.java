package kr.co.seoulit.his.support.domain.worklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * v6: Clinical(Order 생성) -> Support(Worklist 자동 생성) 고정용 요청 DTO
 */
public record CreateWorklistTaskRequest(
        @NotNull Long orderId,
        @NotNull Long visitId,
        @NotBlank String category,
        String status,
        String primaryItemCode,
        String primaryItemName
) {}
