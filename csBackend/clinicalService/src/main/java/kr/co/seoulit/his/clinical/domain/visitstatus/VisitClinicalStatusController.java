package kr.co.seoulit.his.clinical.domain.visitstatus;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Phase 2: Visit clinical 상태 조회 API
 *
 * GET /clinical/visit-status/{visitId}
 * → { "visitId": 1, "clinicalStatus": "FINAL_ORDER_READY" }
 *
 * 프론트 오더 화면에서 이 API를 폴링하여 최종오더 액션 활성화 여부 결정
 */
@RestController
@RequestMapping("/clinical/visit-status")
@RequiredArgsConstructor
public class VisitClinicalStatusController {

    private final VisitClinicalStatusService service;

    @GetMapping("/{visitId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long visitId) {
        String status = service.getStatus(visitId);
        return ResponseEntity.ok(Map.of(
                "visitId", visitId,
                "clinicalStatus", status
        ));
    }
}
