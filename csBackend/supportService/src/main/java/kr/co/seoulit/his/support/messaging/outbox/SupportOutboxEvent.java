package kr.co.seoulit.his.support.messaging.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "support_outbox_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_support_outbox_dedup", columnNames = {"dedup_key"})
        },
        indexes = {
                @Index(name = "ix_support_outbox_status", columnList = "status,id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SupportOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 32)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "dedup_key", nullable = false, length = 128)
    private String dedupKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
