package kr.co.seoulit.his.admin.domain.frontoffice.reservation.dto;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        Long patientId,
        String patientName,
        String departmentCode,
        String doctorId,
        LocalDateTime scheduledAt,
        String status,
        Long visitId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime canceledAt,
        String cancelReason
) {}
