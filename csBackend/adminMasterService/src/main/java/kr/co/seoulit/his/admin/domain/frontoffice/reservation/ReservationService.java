package kr.co.seoulit.his.admin.domain.frontoffice.reservation;

import kr.co.seoulit.his.admin.audit.AuditClient;
import kr.co.seoulit.his.admin.global.exception.BusinessException;
import kr.co.seoulit.his.admin.global.exception.ErrorCode;
import kr.co.seoulit.his.admin.domain.frontoffice.reservation.dto.ReservationCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.reservation.dto.ReservationResponse;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.VisitService;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservations;
    private final VisitService visits;
    private final AuditClient audit;

    @Transactional
    public ReservationResponse create(ReservationCreateRequest req) {
        Reservation r = Reservation.builder()
                .patientId(req.patientId())
                .patientName(req.patientName())
                .departmentCode(req.departmentCode())
                .doctorId(req.doctorId())
                .scheduledAt(req.scheduledAt())
                .status("BOOKED")
                .createdAt(LocalDateTime.now())
                .build();
        Reservation saved = reservations.save(r);

        audit.write("RESERVATION_CREATED", "RESERVATION", String.valueOf(saved.getReservationId()), null,
                Map.of("patientId", String.valueOf(saved.getPatientId()), "scheduledAt", String.valueOf(saved.getScheduledAt())));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long id) {
        Reservation r = reservations.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Reservation not found. id=" + id));
        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> list(String status, LocalDate date) {
        List<Reservation> list;
        if (date != null) {
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay();
            list = reservations.findByScheduledAtBetween(from, to);
        } else if (status != null && !status.isBlank()) {
            list = reservations.findByStatus(status.trim().toUpperCase());
        } else {
            list = reservations.findAll();
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReservationResponse cancel(Long id, String reason) {
        Reservation r = reservations.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Reservation not found. id=" + id));

        if (!"BOOKED".equals(norm(r.getStatus()))) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only BOOKED reservation can be canceled. status=" + r.getStatus());
        }

        r.setStatus("CANCELED");
        r.setCanceledAt(LocalDateTime.now());
        r.setCancelReason(reason);
        r.setUpdatedAt(LocalDateTime.now());
        Reservation saved = reservations.save(r);

        audit.write("RESERVATION_CANCELED", "RESERVATION", String.valueOf(saved.getReservationId()), null,
                Map.of("reason", reason == null ? "" : reason));

        return toResponse(saved);
    }

    /**
     * 체크인: 예약 -> 접수(Visit) 생성 + 대기표 자동 생성(VisitService에서 수행)
     */
    @Transactional
    public ReservationResponse checkIn(Long id) {
        Reservation r = reservations.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Reservation not found. id=" + id));

        if (!"BOOKED".equals(norm(r.getStatus()))) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only BOOKED reservation can be checked in. status=" + r.getStatus());
        }

        var visit = visits.create(new VisitCreateRequest(
                r.getPatientId(),
                r.getPatientName(),
                r.getDepartmentCode(),
                r.getDoctorId(),
                null,
                null
        ));

        r.setStatus("CHECKED_IN");
        r.setVisitId(visit.visitId());
        r.setUpdatedAt(LocalDateTime.now());
        Reservation saved = reservations.save(r);

        audit.write("RESERVATION_CHECKED_IN", "RESERVATION", String.valueOf(saved.getReservationId()), null,
                Map.of("visitId", null));

        return toResponse(saved);
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getReservationId(),
                r.getPatientId(),
                r.getPatientName(),
                r.getDepartmentCode(),
                r.getDoctorId(),
                r.getScheduledAt(),
                r.getStatus(),
                r.getVisitId(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getCanceledAt(),
                r.getCancelReason()
        );
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}
