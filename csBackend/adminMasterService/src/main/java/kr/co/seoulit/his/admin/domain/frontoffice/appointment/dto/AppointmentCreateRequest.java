package kr.co.seoulit.his.admin.domain.frontoffice.appointment.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        @NotNull Long patientId,
        String patientName,
        String departmentCode,
        String doctorId,
        @NotNull LocalDateTime scheduledAt
) {}
