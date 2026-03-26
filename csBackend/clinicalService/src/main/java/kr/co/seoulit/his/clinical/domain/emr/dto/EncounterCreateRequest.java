package kr.co.seoulit.his.clinical.domain.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EncounterCreateRequest(
        @NotNull Long visitId,
        @NotBlank String note
) {}
