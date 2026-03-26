package kr.co.seoulit.his.admin.domain.frontoffice.appointment.dto;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long appointmentId,
        Long patientId,
        String patientName,
        String departmentCode,
        String doctorId,
        String status,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
