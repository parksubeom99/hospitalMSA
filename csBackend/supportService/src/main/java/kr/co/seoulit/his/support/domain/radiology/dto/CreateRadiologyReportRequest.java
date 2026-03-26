package kr.co.seoulit.his.support.domain.radiology.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRadiologyReportRequest(
        @NotNull Long orderId,
        @NotBlank String reportText,
        @NotBlank String idempotencyKey
) {}
