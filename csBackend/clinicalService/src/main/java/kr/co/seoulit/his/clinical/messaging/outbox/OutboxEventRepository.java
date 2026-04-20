package kr.co.seoulit.his.clinical.messaging.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findAllByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    // [ADDED] 중복 발행 방지 — 동일 eventType + partitionKey + status 조합 존재 여부
    boolean existsByEventTypeAndPartitionKeyAndStatus(String eventType, String partitionKey, String status);
}
