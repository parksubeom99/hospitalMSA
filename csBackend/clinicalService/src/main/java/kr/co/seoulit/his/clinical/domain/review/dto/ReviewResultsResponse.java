package kr.co.seoulit.his.clinical.domain.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResultsResponse(
        Long visitId,
        List<ReviewOrderResult> orders
) {

    public record ReviewOrderResult(
            Long orderId,
            String category,
            String status,
            LocalDateTime createdAt,
            List<ResultEntry> results
    ) {}

    public record ResultEntry(
            String kind,           // LAB/RAD/PROC
            Long resultId,
            String text,
            String status,
            LocalDateTime createdAt
    ) {}
}
