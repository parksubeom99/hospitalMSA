package kr.co.seoulit.his.admin.domain.master.examcatalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "master_exam_catalog",
        uniqueConstraints = @UniqueConstraint(name="uk_master_exam_catalog_code", columnNames = {"item_code"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ExamCatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="exam_catalog_id")
    private Long examCatalogId;

    @Column(name="item_code", nullable=false, length=50)
    private String itemCode; // e.g., LAB_BLOOD, RAD_CT, PROC_ENDOSCOPY

    @Column(name="category", nullable=false, length=20)
    private String category; // LAB/RAD/PROC

    @Column(name="display_name_kr", nullable=false, length=100)
    private String displayNameKr;

    @Builder.Default
    @Column(name="active", nullable=false)
    private boolean active = true;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
