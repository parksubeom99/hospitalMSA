package kr.co.seoulit.his.admin.domain.frontoffice.visit.dto;

import jakarta.validation.constraints.NotBlank;

public record VisitStatusUpdateRequest(
        @NotBlank String status
) {}
