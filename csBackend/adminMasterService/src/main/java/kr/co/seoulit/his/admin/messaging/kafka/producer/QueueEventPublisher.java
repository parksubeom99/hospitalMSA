package kr.co.seoulit.his.admin.messaging.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QueueEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${admin.kafka.topic.queue:queue.events}")
    private String topic;

    public void publish(String eventType, Long visitId, String category, Long ticketId, Long orderId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("eventType", eventType);
            payload.put("visitId", visitId);
            payload.put("category", category);
            payload.put("ticketId", ticketId);
            if (orderId != null) payload.put("orderId", orderId);
            payload.put("occurredAt", Instant.now().toString());

            String json = objectMapper.writeValueAsString(payload);
            kafka.send(topic, String.valueOf(visitId), json);
        } catch (Exception e) {
            // ignore (demo environment)
        }
    }
}
