package kr.co.seoulit.his.admin.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.admin.domain.frontoffice.queue.QueueService;
import kr.co.seoulit.his.admin.integration.clinical.client.ClinicalClient;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueEventConsumer {

    private final QueueService queues;
    private final ClinicalClient clinical;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = "${admin.kafka.topic.queue:queue.events}")
    public void onMessage(String message) {
        try {
            JsonNode n = mapper.readTree(message);

            String eventId = text(n, "eventId");
            String eventType = text(n, "eventType");

            Long visitId = longVal(n, "visitId");
            Long orderId = longVal(n, "orderId");
            String category = text(n, "category");

            // visitId가 없고 orderId만 있는 경우: Clinical에서 조회해서 보강
            if (visitId == null && orderId != null) {
                visitId = clinical.fetchVisitIdByOrderId(orderId);
            }
            if (visitId == null) return;

            // 상태 매핑 (Support/FrontOffice 이벤트 모두 수용)
            String mapped = mapStatus(eventType, text(n, "status"));
            queues.applyEvent(eventId, eventType, visitId, category, mapped);

        } catch (Exception ignore) {
        }
    }

    private static String mapStatus(String eventType, String status) {
        String et = eventType == null ? "" : eventType.trim().toUpperCase();
        String st = status == null ? "" : status.trim().toUpperCase();

        // 우선 status 필드가 있으면 반영
        if (st.equals("IN_PROGRESS")) return "IN_PROGRESS";
        if (st.equals("DONE") || st.equals("COMPLETED") || st.equals("FINISHED")) return "DONE";
        if (st.equals("CALLED")) return "CALLED";
        if (st.equals("WAITING") || st.equals("NEW")) return "WAITING";

        // 이벤트 타입 기반 매핑
        if (et.contains("CALLED") || et.contains("STARTED")) return "IN_PROGRESS";
        if (et.contains("COMPLETED") || et.contains("DONE") || et.contains("RECORDED")) return "DONE";

        return "";
    }

    private static String text(JsonNode n, String key) {
        JsonNode v = n.get(key);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Long longVal(JsonNode n, String key) {
        JsonNode v = n.get(key);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.asLong();
        try { return Long.parseLong(v.asText()); } catch (Exception e) { return null; }
    }
}
