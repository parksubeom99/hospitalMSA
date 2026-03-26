package kr.co.seoulit.his.clinical.domain.emr.soap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit_soap")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoapNote {

    @Id
    private Long visitId; // visit_id = PK(FK)

    @Column(length = 4000)
    private String subjective;

    @Column(length = 4000)
    private String objective;

    @Column(length = 4000)
    private String assessment;

    @Column(length = 4000)
    private String plan;

    // =========================
    // [ADDED] Version & Archive fields (append-only history)
    // =========================
    @Builder.Default
    @Column(nullable = false)
    private Integer versionNo = 1;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean archived = false;

    private LocalDateTime archivedAt;

    @Column(length = 100)
    private String archivedBy; // loginId

    @Column(length = 255)
    private String archivedReason;
}
