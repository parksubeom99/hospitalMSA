package kr.co.seoulit.his.clinical.domain.emr.soap.dto;

import java.time.LocalDateTime;

public record SoapResponse(
        Long visitId,
        String subjective,
        String objective,
        String assessment,
        String plan,

        Integer versionNo,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime archivedAt,
        String archivedBy,
        String archivedReason
) {}
