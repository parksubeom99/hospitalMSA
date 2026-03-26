package kr.co.seoulit.his.admin.domain.execution;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="surgery_exec",
        indexes = {
                @Index(name="idx_surg_final_order", columnList = "final_order_id"),
                @Index(name="idx_surg_idem", columnList = "idempotency_key", unique = true)
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SurgeryExec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surgeryExecId;

    @Column(nullable = false)
    private Long finalOrderId;

    @Column(nullable = false, length = 128)
    private String surgeryName;

    @Column(length = 64)
    private String room;

    @Column(length = 80)
    private String surgeon;

    /** NEW -> IN_PROGRESS(=scheduled) -> DONE */
    @Column(nullable = false, length = 32)
    private String status;

    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
