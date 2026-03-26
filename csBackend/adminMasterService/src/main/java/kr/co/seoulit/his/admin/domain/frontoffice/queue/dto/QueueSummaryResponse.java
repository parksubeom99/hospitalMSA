package kr.co.seoulit.his.admin.domain.frontoffice.queue.dto;

import java.time.LocalDateTime;

public record QueueSummaryResponse(
        String category,
        int waitingCount,
        int sampleSize,
        int avgServiceMinutes,
        int estimatedMinutesForNew,
        LocalDateTime calculatedAt
) {}
