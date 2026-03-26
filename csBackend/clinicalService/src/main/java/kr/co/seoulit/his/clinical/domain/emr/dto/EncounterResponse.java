package kr.co.seoulit.his.clinical.domain.emr.dto;

import java.time.LocalDateTime;

public record EncounterResponse(
        Long encounterNoteId,
        Long visitId,
        String note,
        LocalDateTime createdAt,

        // [ADDED]
        LocalDateTime updatedAt,
        String updatedBy,
        boolean archived,
        LocalDateTime archivedAt,
        String archivedBy,
        String archivedReason
) {}
