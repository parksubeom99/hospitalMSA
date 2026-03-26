package kr.co.seoulit.his.admin.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationOutboxKafkaPublisher {

    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.publisher.max-retries:10}")
    private int maxRetries;

    /**
     * ✅ NEW 이벤트를 Kafka로 발행 후 PUBLISHED 마킹
     * - 실패 시 failCount++, lastError 저장
     * - maxRetries 초과 시 FAILED로 고정(운영자가 outbox API로 확인)
     */
    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:2000}")
    @Transactional
    public void publishNew() {
        var list = repo.findAllByStatusOrderByCreatedAtAsc("NEW", org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(batchSize, 500))));
        if (list.isEmpty()) return;

        for (OutboxEvent e : list) {
            try {
                if (e.getFailCount() >= maxRetries) {
                    e.setStatus("FAILED");
                    continue;
                }

                String key = (e.getPartitionKey() == null || e.getPartitionKey().isBlank()) ? e.getAggregateId() : e.getPartitionKey();

                Map<String, Object> envelope = Map.of(
                        "eventId", e.getEventId(),
                        "eventType", e.getEventType(),
                        "aggregateType", e.getAggregateType(),
                        "aggregateId", e.getAggregateId(),
                        "partitionKey", key,
                        "occurredAt", String.valueOf(e.getCreatedAt()),
                        "payload", om.readTree(e.getPayload())
                );

                String json = om.writeValueAsString(envelope);

                // sync send: 성공 확인 후에만 PUBLISHED 처리
                kafkaTemplate.send(e.getTopic(), key, json).get();

                e.setStatus("PUBLISHED");
                e.setPublishedAt(LocalDateTime.now());
                e.setLastError(null);

            } catch (Exception ex) {
                e.setFailCount(e.getFailCount() + 1);
                e.setLastError(ex.getMessage());
                log.warn("Outbox publish failed: id={}, eventId={}, topic={}, failCount={}, err={}",
                        e.getId(), e.getEventId(), e.getTopic(), e.getFailCount(), ex.getMessage());
            }
        }
    }
}
