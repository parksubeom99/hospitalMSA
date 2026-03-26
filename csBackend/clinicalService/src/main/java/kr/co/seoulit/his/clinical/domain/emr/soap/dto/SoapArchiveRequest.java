package kr.co.seoulit.his.clinical.domain.emr.soap.dto;

import jakarta.validation.constraints.Size;

public record SoapArchiveRequest(
        @Size(max = 255) String reason
) {}
