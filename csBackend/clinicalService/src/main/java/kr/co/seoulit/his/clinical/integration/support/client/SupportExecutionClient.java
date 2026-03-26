package kr.co.seoulit.his.clinical.integration.support.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SupportExecutionClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${support.base-url}")
    private String supportBaseUrl;

    private HttpHeaders forwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null) headers.set(HttpHeaders.AUTHORIZATION, auth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public void createInjectionTask(Long finalOrderId, String idempotencyKey, String note) {
        String url = supportBaseUrl + "/injections/tasks";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                Map.of("finalOrderId", finalOrderId, "idempotencyKey", idempotencyKey, "note", note),
                forwardHeaders()
        );
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    public void createMedExecTask(Long finalOrderId, String idempotencyKey, String note) {
        String url = supportBaseUrl + "/med-execs/tasks";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                Map.of("finalOrderId", finalOrderId, "idempotencyKey", idempotencyKey, "note", note),
                forwardHeaders()
        );
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}
