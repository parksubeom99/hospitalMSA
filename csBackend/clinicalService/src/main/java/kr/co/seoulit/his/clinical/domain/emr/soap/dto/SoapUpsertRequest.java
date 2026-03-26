package kr.co.seoulit.his.clinical.domain.emr.soap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SoapUpsertRequest(
        @NotBlank @Size(max = 4000) String subjective,
        @Size(max = 4000) String objective,
        @Size(max = 4000) String assessment,
        @Size(max = 4000) String plan
) {}
