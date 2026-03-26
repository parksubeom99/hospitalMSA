package kr.co.seoulit.his.admin.messaging.kafka;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "consumer_processed_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_processed_event_id", columnNames = "event_id")
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
