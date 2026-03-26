package kr.co.seoulit.his.admin.domain.master.patient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(
        Long patientId,
        String name,
        String gender,
        String rrnMasked,
        LocalDate birthDate,
        String phone,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
