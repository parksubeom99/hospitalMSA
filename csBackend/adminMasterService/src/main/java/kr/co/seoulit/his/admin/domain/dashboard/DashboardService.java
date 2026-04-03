package kr.co.seoulit.his.admin.domain.dashboard;

import kr.co.seoulit.his.admin.domain.config.AdminConfigService;
import kr.co.seoulit.his.admin.domain.dashboard.dto.DashboardSummaryResponse;
import kr.co.seoulit.his.admin.domain.frontoffice.appointment.Appointment;
import kr.co.seoulit.his.admin.domain.frontoffice.appointment.AppointmentRepository;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.Visit;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VisitRepository visits;
    private final AppointmentRepository appts;
    private final AdminConfigService adminConfigService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate date) {
        // Visits: 기본은 '오늘 createdAt' 기준. (createdAt이 null인 레거시는 집계에서 제외)
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<Visit> allVisits = visits.findAll().stream()
                // [CHANGED] typo fix: nul -> null
                .filter(v -> v.getCreatedAt() != null)
                .filter(v -> !v.getCreatedAt().isBefore(from) && v.getCreatedAt().isBefore(to))
                .toList();

        // =========================
        // KPI: waiting / reservation / emergency
        // =========================

        // [CHANGED] 대기 정책: VisitService가 READY를 WAITING으로 정규화하고,
        // 신규 생성 직후 WAITING으로 승격하므로 WAITING(+CALLED)을 대기 범위로 집계합니다.
        int waiting = (int) allVisits.stream()
                .filter(v -> v.getStatus() != null)
                .filter(v -> {
                    String st = v.getStatus().trim().toUpperCase();
                    if ("READY".equals(st)) st = "WAITING"; // 레거시 호환
                    return "WAITING".equals(st) || "CALLED".equals(st);
                })
                .count();

        // [FIXED] 응급은 운영자가 수동 설정한 값 사용 (admin_config 테이블)
        //         visit 데이터 집계 아님 — 설계 원칙: 응급 병상 수는 운영자 직접 조정
        int emergency = adminConfigService.getEmergencyCount();

        List<Appointment> todayAppts = appts.findByStatusAndScheduledAtBetween("BOOKED", from, to);
        int reservation = todayAppts.size();

        // 환자 보드: CLOSED/CANCELED 제외, 최신 12명
        List<DashboardSummaryResponse.PatientBoardRow> board = allVisits.stream()
                .filter(v -> v.getStatus() == null || !("CANCELED".equalsIgnoreCase(v.getStatus()) || "CLOSED".equalsIgnoreCase(v.getStatus()) || "COMPLETED".equalsIgnoreCase(v.getStatus())))
                .sorted(Comparator.comparing(Visit::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(12)
                .map(v -> new DashboardSummaryResponse.PatientBoardRow(
                        v.getVisitId(),
                        v.getPatientId(),
                        v.getPatientName(),
                        v.getDepartmentCode(),
                        v.getDoctorId(),
                        v.getStatus(),
                        v.getArrivalType(),
                        v.getTriageLevel(),
                        v.getCreatedAt()
                ))
                .toList();

        return new DashboardSummaryResponse(
                new DashboardSummaryResponse.Counts(waiting, reservation, emergency),
                board,
                LocalDateTime.now()
        );
    }
}
