package kr.co.seoulit.his.support.integration.clinical.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class FinalOrderClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${order.base-url:http://localhost:8184}")
    private String clinicalBaseUrl; // same service(Clinical) base

    private HttpHeaders forwardHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null) headers.set(HttpHeaders.AUTHORIZATION, auth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

        public String getStatus (Long finalOrderId){
            String url = clinicalBaseUrl + "/final-orders/" + finalOrderId;
            HttpEntity<Void> entity = new HttpEntity<>(forwardHeaders());
            ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Object status = (res.getBody() == null ? null : res.getBody().get("status"));
            return status == null ? null : String.valueOf(status);
        }


    public void updateStatus(Long finalOrderId, String status) {
        String url = clinicalBaseUrl + "/final-orders/" + finalOrderId + "/status";
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("status", status), forwardHeaders());
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }
}
