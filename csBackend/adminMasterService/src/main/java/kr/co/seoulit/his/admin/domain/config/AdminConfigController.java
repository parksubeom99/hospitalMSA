package kr.co.seoulit.his.admin.domain.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigService service;

    /**
     * 응급 병상 수 조회
     * GET /admin/config/emergency-count
     * Response: {"value": 3}
     */
    @GetMapping("/emergency-count")
    public ResponseEntity<Map<String, Integer>> getEmergencyCount() {
        return ResponseEntity.ok(Map.of("value", service.getEmergencyCount()));
    }

    /**
     * 응급 병상 수 설정 (운영자 수동 조정)
     * PUT /admin/config/emergency-count
     * Body: {"value": 7}
     * Response: {"value": 7}
     */
    @PutMapping("/emergency-count")
    public ResponseEntity<Map<String, Integer>> setEmergencyCount(@RequestBody Map<String, Integer> body) {
        int requested = body.getOrDefault("value", 0);
        int saved = service.setEmergencyCount(requested);
        return ResponseEntity.ok(Map.of("value", saved));
    }
}
