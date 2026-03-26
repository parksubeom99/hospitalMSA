package kr.co.seoulit.his.support.domain.procedure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProcedureReportRequest(
        @NotNull Long orderId,
        @NotBlank String reportText,
        @NotBlank String idempotencyKey
) {}
