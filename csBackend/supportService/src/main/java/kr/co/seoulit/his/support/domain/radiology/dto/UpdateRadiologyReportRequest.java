package kr.co.seoulit.his.support.domain.radiology.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRadiologyReportRequest(
        @NotBlank(message = "reportText is required")
        String reportText,
        String reason
) {}
