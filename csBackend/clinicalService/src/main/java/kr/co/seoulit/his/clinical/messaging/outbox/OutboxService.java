package kr.co.seoulit.his.clinical.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    /**
     * ✅ Step9: Transactional Outbox 표준 형태
     * - 업무 트랜잭션 안에서 OutboxEvent(NEW) 저장
     * - Publisher(@Scheduled)가 NEW를 Kafka로 발행 후 PUBLISHED 마킹
     */
    @Transactional
    public void record(String eventType,
                       String aggregateType,
                       String aggregateId,
                       String partitionKey,
                       String topic,
                       Map<String, Object> payload) {
        if (topic == null || topic.isBlank()) return;
        try {
            String json = om.writeValueAsString(payload == null ? Map.of() : payload);
            repo.save(OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .partitionKey(partitionKey)
                    .topic(topic)
                    .payload(json)
                    .status("NEW")
                    .failCount(0)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            // Outbox 실패가 업무 흐름을 막지 않도록 "best-effort"
        }
    }

    // [ADDED] 중복 발행 방지 record
    // 이유: DIAGNOSTIC_ORDER_SUBMITTED는 SoapNoteService + OrderService 양쪽에서
    //       트리거될 수 있음. visitStatusSvc.markExamRequested()가 idempotent이지만
    //       Outbox 레벨에서도 동일 eventType + partitionKey(=visitId) + status=NEW 레코드
    //       존재 시 중복 삽입을 방어적으로 차단.
    @Transactional
    public void recordIfAbsent(String eventType,
                               String aggregateType,
                               String aggregateId,
                               String partitionKey,
                               String topic,
                               Map<String, Object> payload) {
        if (topic == null || topic.isBlank()) return;
        // 동일 eventType + partitionKey 조합의 NEW 레코드가 이미 있으면 skip
        boolean exists = repo.existsByEventTypeAndPartitionKeyAndStatus(eventType, partitionKey, "NEW");
        if (exists) return;
        record(eventType, aggregateType, aggregateId, partitionKey, topic, payload);
    }

    /** [LEGACY][STEP8] 기존 시그니처 유지(호출부 최소화를 위한 안전장치) */
    @Transactional
    public void record(String eventType, String aggregateType, String aggregateId, Map<String, Object> payload) {
        record(eventType, aggregateType, aggregateId, aggregateId, null, payload);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> listNew(int limit) {
        return repo.findAllByStatusOrderByCreatedAtAsc("NEW", PageRequest.of(0, Math.max(1, Math.min(limit, 500))));
    }

    @Transactional
    public void markPublished(Long id) {
        OutboxEvent e = repo.findById(id).orElseThrow();
        e.setStatus("PUBLISHED");
        e.setPublishedAt(LocalDateTime.now());
        e.setLastError(null);
    }

    @Transactional
    public void markFailed(Long id, String error) {
        OutboxEvent e = repo.findById(id).orElseThrow();
        e.setStatus("FAILED");
        e.setLastError(error);
        e.setFailCount(e.getFailCount() + 1);
    }
}
