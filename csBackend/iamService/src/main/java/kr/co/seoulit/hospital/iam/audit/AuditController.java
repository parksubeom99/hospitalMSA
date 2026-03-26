package kr.co.seoulit.hospital.iam.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/audit")
public class AuditController {

    private final AuditLogRepository repo;

    public record AuditRequest(
            String eventId,
            String actorLoginId,
            String serviceName,
            String action,
            String result,
            String targetType,
            String targetId,
            Long patientId,
            String detailJson
    ) {}

    // 다른 서비스들이 REST로 감사로그 적재(프로젝트 기본)
    @PostMapping
    public ResponseEntity<AuditLog> write(@RequestBody AuditRequest req, HttpServletRequest httpReq) {
        String eventId = (req.eventId() == null || req.eventId().isBlank()) ? UUID.randomUUID().toString() : req.eventId();
        String ip = extractClientIp(httpReq);
        String ua = httpReq.getHeader("User-Agent");

        AuditLog log = AuditLog.builder()
                .eventId(eventId)
                .actorLoginId(req.actorLoginId() == null ? "UNKNOWN" : req.actorLoginId())
                .serviceName(req.serviceName() == null || req.serviceName().isBlank() ? "UNKNOWN" : req.serviceName())
                .action(req.action() == null ? "UNKNOWN" : req.action())
                .result(req.result() == null || req.result().isBlank() ? "SUCCESS" : req.result())
                .targetType(req.targetType())
                .targetId(req.targetId())
                .patientId(req.patientId())
                .ipAddress(ip)
                .userAgent(ua)
                .detailJson(req.detailJson())
                .createdAt(LocalDateTime.now())
                .archived(false)
                .build();
        return ResponseEntity.ok(repo.save(log));
    }

    // 단순 전체 목록(내부 SYS 전용) - 대체로 /audit/logs 사용 권장
    @GetMapping
    @PreAuthorize("hasRole('SYS')")
    public ResponseEntity<List<AuditLog>> list() {
        return ResponseEntity.ok(repo.findAll());
    }

    private String extractClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        String xr = req.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) return xr.trim();
        return req.getRemoteAddr();
    }
}
