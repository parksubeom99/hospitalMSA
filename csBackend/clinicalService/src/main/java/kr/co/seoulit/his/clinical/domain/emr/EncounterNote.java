package kr.co.seoulit.his.clinical.domain.emr;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="encounter_note")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EncounterNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noteId;

    @Column(nullable=false)
    private Long visitId;

    @Column(nullable=false, length=4000)
    private String note;

    @Column(nullable=false)
    private LocalDateTime createdAt;

    // =========================
    // [ADDED] Update & Archive fields
    // =========================
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String updatedBy; // loginId

    @Builder.Default
    @Column(nullable = false)
    private boolean archived = false;

    private LocalDateTime archivedAt;

    @Column(length = 100)
    private String archivedBy; // loginId

    @Column(length = 255)
    private String archivedReason;
}
