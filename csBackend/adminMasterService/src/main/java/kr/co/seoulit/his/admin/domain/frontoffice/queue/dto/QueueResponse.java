package kr.co.seoulit.his.admin.domain.frontoffice.queue.dto;

import java.time.LocalDateTime;

public record QueueResponse(
        Long ticketId,
        Long visitId,
        String category,
        String ticketNo,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime calledAt,
        LocalDateTime doneAt
) {}
