package kr.co.seoulit.hospital.iam.auth;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.seoulit.hospital.iam.JsonUtil;
import kr.co.seoulit.hospital.iam.audit.AuditLog;
import kr.co.seoulit.hospital.iam.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import kr.co.seoulit.hospital.global.common.ApiErrorResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditLogRepository auditRepo;

    public record LoginRequest(String loginId, String username, String password) {}
    public record RefreshRequest(String refreshToken) {}
    public record LogoutRequest(String refreshToken) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest httpReq) {
        String ip = extractClientIp(httpReq);
        String ua = httpReq.getHeader("User-Agent");
        String loginId = req.loginId() != null && !req.loginId().isBlank() ? req.loginId() : req.username();

        try {
            AuthService.LoginResult result = authService.login(loginId, req.password());

            auditRepo.save(AuditLog.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .actorLoginId(result.loginId())
                    .serviceName("IAM")
                    .action("LOGIN")
                    .result("SUCCESS")
                    .targetType("AUTH")
                    .targetId(null)
                    .patientId(null)
                    .ipAddress(ip)
                    .userAgent(ua)
                    .detailJson(JsonUtil.toJson(
                            Map.of("roles", result.roles(), "staffId", result.staffId(), "perms", result.permissions())
                    ))
                    .createdAt(LocalDateTime.now())
                    .archived(false)
                    .build());

            return ResponseEntity.ok(result);
        } catch (AuthService.AuthFailedException afe) {
            auditRepo.save(AuditLog.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .actorLoginId(loginId)
                    .serviceName("IAM")
                    .action("LOGIN")
                    .result("FAIL")
                    .targetType("AUTH")
                    .targetId(null)
                    .patientId(null)
                    .ipAddress(ip)
                    .userAgent(ua)
                    .detailJson(JsonUtil.toJson(
                            Map.of("reason", afe.getReason().name(), "hint", "internal-only", "loginId", loginId)
                    ))
                    .createdAt(LocalDateTime.now())
                    .archived(false)
                    .build());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("AUTH_FAILED", "로그인에 실패했습니다.", "/auth/login", LocalDateTime.now()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        try {
            return ResponseEntity.ok(authService.refresh(req.refreshToken()));
        } catch (AuthService.AuthFailedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse("INVALID_REFRESH_TOKEN", "토큰 갱신에 실패했습니다.", "/auth/refresh", LocalDateTime.now()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest req) {
        authService.logoutByRefreshToken(req.refreshToken());
        return ResponseEntity.ok(Map.of("success", true));
    }

    private String extractClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
        String xr = req.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) return xr.trim();
        return req.getRemoteAddr();
    }
}
