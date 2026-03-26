package kr.co.seoulit.his.support.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditClient {

    private final RestTemplate restTemplate;

    @Value("${iam.audit.enabled:true}")
    private boolean enabled;

    @Value("${iam.audit.base-url:http://localhost:8181}")
    private String baseUrl;

    public void write(String action, String targetType, String targetId, Long patientId, Map<String, Object> detail) {
        if (!enabled) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth != null) ? auth.getName() : "anonymous";

        AuditRequest req = new AuditRequest(
                UUID.randomUUID().toString(),
                actor,
                action,
                targetType,
                targetId,
                patientId,
                detail == null ? null : JsonUtil.toJson(detail)
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest httpReq = attrs.getRequest();
                    String authHeader = httpReq.getHeader("Authorization");
                    if (authHeader != null && !authHeader.isBlank()) {
                        headers.set("Authorization", authHeader);
                    }
                    String traceId = httpReq.getHeader("X-Trace-Id");
                    if (traceId != null && !traceId.isBlank()) {
                        headers.set("X-Trace-Id", traceId);
                    }
                }
            } catch (Exception ignored) {}

            HttpEntity<AuditRequest> entity = new HttpEntity<>(req, headers);
            restTemplate.exchange(baseUrl + "/audit", HttpMethod.POST, entity, Object.class);
        } catch (Exception ignored) {
            // 1차 기준: 감사로그 실패로 본 기능이 깨지지 않게 한다.
        }
    }

    public record AuditRequest(
            String eventId,
            String actorLoginId,
            String action,
            String targetType,
            String targetId,
            Long patientId,
            String detailJson
    ) {}
}
