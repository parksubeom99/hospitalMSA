package kr.co.seoulit.his.admin.integration.clinical.client;

import kr.co.seoulit.his.admin.integration.iam.facade.SystemJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ClinicalClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final SystemJwtTokenProvider systemJwt;

    @Value("${clinical.base-url}")
    private String baseUrl;

    @SuppressWarnings("unchecked")
    public Long fetchVisitIdByOrderId(Long orderId) {
        String url = baseUrl + "/orders/" + orderId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(systemJwt.issueSystemToken());
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) return null;
        Object v = res.getBody().get("visitId");
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }
}
