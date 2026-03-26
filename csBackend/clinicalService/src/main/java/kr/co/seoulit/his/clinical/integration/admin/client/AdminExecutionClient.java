package kr.co.seoulit.his.clinical.integration.admin.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminExecutionClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${admin.base-url}")
    private String adminBaseUrl;

    private HttpHeaders forwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null) headers.set(HttpHeaders.AUTHORIZATION, auth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public void createAdmissionTask(Long finalOrderId, String idempotencyKey, String ward) {
        String url = adminBaseUrl + "/admissions/tasks";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                Map.of("finalOrderId", finalOrderId, "idempotencyKey", idempotencyKey, "ward", ward),
                forwardHeaders()
        );
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    // =========================
    // [ADDED][STEP7] Surgery task create
    // =========================
    public void createSurgeryTask(Long finalOrderId, String idempotencyKey, String surgeryName, String room) {
        String url = adminBaseUrl + "/surgeries/tasks";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                Map.of(
                        "finalOrderId", finalOrderId,
                        "idempotencyKey", idempotencyKey,
                        "surgeryName", surgeryName,
                        "room", room
                ),
                forwardHeaders()
        );
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}
