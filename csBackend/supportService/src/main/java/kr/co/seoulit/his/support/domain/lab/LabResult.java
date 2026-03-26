package kr.co.seoulit.his.support.domain.lab;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="lab_result")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LabResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long labResultId;

    @Column(nullable=false)
    private Long orderId;

    private String resultText;

    @Column(nullable=false)
    private String status; // DONE (1차)

    @Column(nullable=false, unique = true)
    private String idempotencyKey;

    @Column(nullable=false)
    private LocalDateTime createdAt;

    // ------------------ v3: 수정/아카이브(정책 기반) ------------------
    private LocalDateTime updatedAt;
    private String updatedBy;

    @Column(nullable=false)
    private boolean archived;
    private LocalDateTime archivedAt;
    private String archivedBy;
    private String archivedReason;
}
