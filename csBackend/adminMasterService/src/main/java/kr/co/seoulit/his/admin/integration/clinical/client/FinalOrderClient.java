package kr.co.seoulit.his.admin.integration.clinical.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinalOrderClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${clinical.base-url}")
    private String clinicalBaseUrl;

    private HttpHeaders forwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null) headers.set(HttpHeaders.AUTHORIZATION, auth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    } // ✅ FIX: forwardHeaders() 닫는 중괄호/return 누락으로 메서드 범위가 깨졌던 문제 수정

    public Map<String, Object> getFinalOrder(Long finalOrderId) {
        String url = clinicalBaseUrl + "/final-orders/" + finalOrderId;
        HttpEntity<Void> entity = new HttpEntity<>(forwardHeaders());
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return resp.getBody();
    } // ✅ FIX: 중복 정의된 getFinalOrder() 중 1개만 남김

    public void updateStatus(Long finalOrderId, String status) {
        String url = clinicalBaseUrl + "/final-orders/" + finalOrderId + "/status";
        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(Map.of("status", status), forwardHeaders());
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    } // ✅ FIX: updateStatus() 닫는 중괄호 누락으로 이후 코드가 클래스 밖으로 튀던 문제 수정

} // ✅ FIX: 클래스 닫는 중괄호 정리