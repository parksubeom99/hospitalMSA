package kr.co.seoulit.his.support.domain.procedure.dto;

import java.time.LocalDateTime;

public record ProcedureReportResponse(
        Long procedureReportId,
        Long orderId,
        String reportText,
        String status,
        String idempotencyKey,
        LocalDateTime createdAt
) {}
