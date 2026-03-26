package kr.co.seoulit.his.admin.domain.frontoffice.visit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_visit", indexes = {
        @Index(name = "idx_admin_visit_status", columnList = "status"),
        @Index(name = "idx_admin_visit_patient", columnList = "patientId"),
        // [CHANGED] emergency(B안) KPI/조회용 인덱스
        @Index(name = "idx_admin_visit_arrival_type", columnList = "arrival_type"),
        @Index(name = "idx_admin_visit_triage_level", columnList = "triage_level")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long visitId;

    private Long patientId;
    private String patientName;

    private String departmentCode;
    private String doctorId;

    /**
     * Visit 상태 (원무 관점)
     * CREATED -> WAITING -> CALLED -> COMPLETED
     * + CANCELED
     */
    private String status;

    // =========================
    // Emergency(B안) fields
    // =========================

    /**
     * [CHANGED] 내원 구분
     * - RESERVATION / WALKIN / EMERGENCY
     * - 비응급 방문은 null 허용(기존 데이터 호환)
     */
    @Column(name = "arrival_type", length = 20)
    private String arrivalType;

    /**
     * [CHANGED] 응급 중증도(트리아지)
     * - 보통 1(중증) ~ 5(경증) 범위를 사용
     * - 비응급 방문은 null 허용
     */
    @Column(name = "triage_level")
    private Integer triageLevel;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDateTime canceledAt;
    private String cancelReason;

    private LocalDateTime closedAt;
}
