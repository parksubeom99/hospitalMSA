package kr.co.seoulit.his.admin.domain.master.patientalert.dto;

import java.time.LocalDateTime;

public record PatientAlertResponse(
        Long patientAlertId,
        Long patientId,
        String type,
        String message,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
