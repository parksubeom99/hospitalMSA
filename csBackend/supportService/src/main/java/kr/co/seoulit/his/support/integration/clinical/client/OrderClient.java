package kr.co.seoulit.his.support.integration.clinical.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import kr.co.seoulit.his.support.integration.clinical.facade.OrderStatusSyncFailureService;
import kr.co.seoulit.his.support.integration.clinical.dto.OrderDto; // [CHANGED] missing import fix
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderClient {

    private final RestTemplate restTemplate;
    private final OrderStatusSyncFailureService syncFailureService;

    @Value("${order.base-url:http://localhost:8184}")
    private String orderBaseUrl;

    private HttpHeaders forwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String auth = req.getHeader("Authorization");
                if (auth != null && !auth.isBlank()) {
                    headers.set("Authorization", auth);
                }
                String traceId = req.getHeader("X-Trace-Id");
                if (traceId != null && !traceId.isBlank()) {
                    headers.set("X-Trace-Id", traceId);
                }
            }
        } catch (Exception ignored) {}
        return headers;
    }


    private String currentTraceId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String traceId = req.getHeader("X-Trace-Id");
                if (traceId != null && !traceId.isBlank()) return traceId;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean postStatus(String targetStatus, Long orderId, String traceIdOverride, boolean recordFailure) {
        String path = switch (targetStatus) {
            case "IN_PROGRESS" -> "/in-progress";
            case "RESULTED" -> "/resulted";
            case "DONE" -> "/done";
            default -> throw new IllegalArgumentException("Unsupported targetStatus: " + targetStatus);
        };
        String url = orderBaseUrl + "/orders/" + orderId + path;
        try {
            HttpHeaders headers = forwardHeaders();
            if (traceIdOverride != null && !traceIdOverride.isBlank()) {
                headers.set("X-Trace-Id", traceIdOverride);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            return true;
        } catch (Exception e) {
            log.warn("Failed to mark order {}. url={}", targetStatus, url, e);
            if (recordFailure) {
                syncFailureService.recordFailure(orderId, targetStatus, url, e.getMessage(), traceIdOverride != null ? traceIdOverride : currentTraceId());
            }
            return false;
        }
    }

    public boolean retrySyncStatus(Long orderId, String targetStatus, String traceId) {
        return postStatus(targetStatus, orderId, traceId, false);
    }

    public List<OrderDto> fetchOrders(String status, String category) {
        // [CHANGED] null-safe query construction to avoid malformed URLs during optional filter calls
        String url = orderBaseUrl + "/orders?status=" + (status == null ? "" : status) + "&category=" + (category == null ? "" : category);
        try {
            HttpEntity<Void> entity = new HttpEntity<>(forwardHeaders());
            ResponseEntity<OrderDto[]> res = restTemplate.exchange(url, HttpMethod.GET, entity, OrderDto[].class);
            OrderDto[] body = res.getBody();
            return body == null ? List.of() : Arrays.asList(body);
        } catch (Exception e) {
            // 조회 실패로 기능 전체가 깨지지 않게 빈 리스트로 degrade
            log.warn("Failed to fetch orders. url={}", url, e);
            return List.of();
        }
    }

    public void markInProgress(Long orderId) {
        postStatus("IN_PROGRESS", orderId, null, true);
    }


    public void markResulted(Long orderId) {
        postStatus("RESULTED", orderId, null, true);
    }

    public void markDone(Long orderId) {
        postStatus("DONE", orderId, null, true);
    }
}
