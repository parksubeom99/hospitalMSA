package kr.co.seoulit.his.clinical.domain.emr.soap.dto;

import java.time.LocalDateTime;

public record SoapVersionResponse(
        Long visitId,
        Integer versionNo,
        LocalDateTime capturedAt,
        String capturedBy,
        boolean current
) {}
