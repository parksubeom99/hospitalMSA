package kr.co.seoulit.his.admin.domain.master.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PatientUpsertRequest(
        @NotNull Long patientId,
        @NotBlank String name,
        @NotBlank String gender,
        String rrnMasked,
        LocalDate birthDate,
        String phone,
        Boolean active
) {}
