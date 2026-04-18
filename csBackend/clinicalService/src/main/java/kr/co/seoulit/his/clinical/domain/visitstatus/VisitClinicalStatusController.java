package kr.co.seoulit.his.clinical.domain.visitstatus;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Phase 2: Visit clinical 상태 조회 API
 *
 * GET /clinical/visit-status
 * → [{ "visitId": 11007, "clinicalStatus": "WAITING" }, ...]
 * 프론트 진료화면 드롭다운 목록 표시용 (전체 목록)
 *
 * GET /clinical/visit-status/{visitId}
 * → { "visitId": 1, "clinicalStatus": "FINAL_ORDER_READY" }
 * 프론트 오더 화면에서 이 API를 폴링하여 최종오더 액션 활성화 여부 결정
 */
@RestController
@RequestMapping("/clinical/visit-status")
@RequiredArgsConstructor
public class VisitClinicalStatusController {

    private final VisitClinicalStatusService service;

    // [ADDED] 전체 목록 조회 — 진료화면 드롭다운용
    // JpaRepository.findAll() 활용, BILLED/BILLING_FAILED 제외하여 진료 중인 항목만 반환
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getStatusList() {
        List<Map<String, Object>> result = service.getAllActiveStatuses()
                .stream()
                .map(vcs -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("visitId", vcs.getVisitId());
                    map.put("clinicalStatus", vcs.getClinicalStatus());
                    map.put("patientName", vcs.getPatientName());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{visitId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long visitId) {
        String status = service.getStatus(visitId);
        return ResponseEntity.ok(Map.of(
                "visitId", visitId,
                "clinicalStatus", status
        ));
    }
}
