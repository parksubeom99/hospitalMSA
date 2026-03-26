package kr.co.seoulit.his.admin.domain.frontoffice.reservation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationCreateRequest(
        @NotNull Long patientId,
        String patientName,
        String departmentCode,
        String doctorId,
        @NotNull LocalDateTime scheduledAt
) {}
