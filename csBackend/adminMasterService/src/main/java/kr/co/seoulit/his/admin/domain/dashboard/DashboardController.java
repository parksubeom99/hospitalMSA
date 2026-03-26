package kr.co.seoulit.his.admin.domain.dashboard;

import kr.co.seoulit.his.admin.domain.dashboard.dto.DashboardSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    /**
     * 현업형: 프론트 대시보드가 1번 호출로 KPI(대기/예약/응급) + 환자 보드 데이터를 받게 합니다.
     * 예) GET /admin/dashboard/summary?date=2026-02-16
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(service.getSummary(date));
    }
}
