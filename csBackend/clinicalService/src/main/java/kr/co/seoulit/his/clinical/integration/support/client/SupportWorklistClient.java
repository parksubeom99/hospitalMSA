package kr.co.seoulit.his.clinical.integration.support.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportWorklistClient {

    private final RestTemplate restTemplate;

    @Value("${support.base-url:http://localhost:8185}")
    private String supportBaseUrl;

    private HttpHeaders forwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String auth = req.getHeader("Authorization");
                if (auth != null && !auth.isBlank()) headers.set("Authorization", auth);
                String traceId = req.getHeader("X-Trace-Id");
                if (traceId != null && !traceId.isBlank()) headers.set("X-Trace-Id", traceId);
            }
        } catch (Exception ignored) {}
        return headers;
    }

    /**
     * v6: Order 생성 시 Support에 Worklist task를 upsert 합니다.
     * - Support 장애/네트워크 실패가 있어도 Order 생성은 깨지지 않게 degrade 합니다.
     */
    public void createWorklistTask(Long orderId, Long visitId, String category, String status, String primaryItemCode, String primaryItemName) {
        String url = supportBaseUrl + "/worklist/create";
        try {
            CreateWorklistTaskRequest body = new CreateWorklistTaskRequest(orderId, visitId, category, status, primaryItemCode, primaryItemName);
            HttpEntity<CreateWorklistTaskRequest> entity = new HttpEntity<>(body, forwardHeaders());
            restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
        } catch (Exception e) {
            log.warn("Failed to create support worklist task. url={} orderId={}", url, orderId, e);
        }
    }

    public record CreateWorklistTaskRequest(Long orderId, Long visitId, String category, String status, String primaryItemCode, String primaryItemName) {}
}
