package kr.co.seoulit.his.admin.messaging.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "outbox_event",
        indexes = {
                @Index(name = "ix_outbox_topic_status_created", columnList = "topic,status,created_at"),
                @Index(name = "ix_outbox_event_id", columnList = "event_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Kafka/Idempotency 기준이 되는 이벤트 ID (UUID)
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 32)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    // ✅ Kafka 토픽 (Publisher가 그대로 사용)
    @Column(name = "topic", nullable = false, length = 128)
    private String topic;

    // ✅ Kafka partition key (없으면 aggregateId 사용)
    @Column(name = "partition_key", length = 128)
    private String partitionKey;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // NEW / PUBLISHED / FAILED

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "last_error", length = 4000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
