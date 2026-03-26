package kr.co.seoulit.his.clinical.integration.support.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportResultClient {

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

    public List<LabResultDto> listLabResults(Long orderId) {
        if (orderId == null) return List.of();
        String url = supportBaseUrl + "/lab-results?orderId=" + orderId;
        return exchangeList(url, new ParameterizedTypeReference<List<LabResultDto>>() {});
    }

    public List<RadiologyReportDto> listRadiologyReports(Long orderId) {
        if (orderId == null) return List.of();
        String url = supportBaseUrl + "/radiology-reports?orderId=" + orderId;
        return exchangeList(url, new ParameterizedTypeReference<List<RadiologyReportDto>>() {});
    }

    public List<ProcedureReportDto> listProcedureReports(Long orderId) {
        if (orderId == null) return List.of();
        String url = supportBaseUrl + "/procedure-reports?orderId=" + orderId;
        return exchangeList(url, new ParameterizedTypeReference<List<ProcedureReportDto>>() {});
    }

    private <T> List<T> exchangeList(String url, ParameterizedTypeReference<List<T>> typeRef) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(forwardHeaders());
            ResponseEntity<List<T>> res = restTemplate.exchange(url, HttpMethod.GET, entity, typeRef);
            return res.getBody() == null ? List.of() : res.getBody();
        } catch (Exception e) {
            log.warn("Failed to fetch support results. url={}, reason={}", url, e.getMessage());
            return List.of();
        }
    }

    // --------- DTOs (Support JSON contract mirror) ---------
    public record LabResultDto(
            Long labResultId,
            Long orderId,
            String resultText,
            String status,
            String idempotencyKey,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String updatedBy,
            boolean archived,
            LocalDateTime archivedAt,
            String archivedBy,
            String archivedReason
    ) {}

    public record RadiologyReportDto(
            Long reportId,
            Long orderId,
            String reportText,
            String status,
            String idempotencyKey,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String updatedBy,
            boolean archived,
            LocalDateTime archivedAt,
            String archivedBy,
            String archivedReason
    ) {}

    public record ProcedureReportDto(
            Long procedureReportId,
            Long orderId,
            String reportText,
            String status,
            String idempotencyKey,
            LocalDateTime createdAt
    ) {}
}
