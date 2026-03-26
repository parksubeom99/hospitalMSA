package kr.co.seoulit.his.support.domain.radiology;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="radiology_report")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RadiologyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long radiologyReportId;

    @Column(nullable=false)
    private Long orderId;

    private String reportText;

    @Column(nullable=false)
    private String status;

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
