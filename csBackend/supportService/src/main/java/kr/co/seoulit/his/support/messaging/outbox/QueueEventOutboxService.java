package kr.co.seoulit.his.support.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueEventOutboxService {

    private final SupportOutboxEventRepository repo;
    private final ObjectMapper objectMapper;

    /**
     * Idempotency(중복 방지): dedupKey 로 UNIQUE 보장
     */
    @Transactional
    public void enqueue(String eventType, Long orderId, String category, Map<String, Object> extras) {
        String dedupKey = eventType + ":ORDER:" + orderId;
        if (repo.existsByDedupKey(dedupKey)) {
            return;
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("orderId", orderId);
        payload.put("category", category);
        payload.put("occurredAt", LocalDateTime.now().toString());
        if (extras != null) {
            payload.putAll(extras);
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("outbox payload serialize failed", e);
        }

        SupportOutboxEvent e = SupportOutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateType("QUEUE")
                .aggregateId(String.valueOf(orderId))
                .payload(json)
                .dedupKey(dedupKey)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        repo.save(e);
    }
}
