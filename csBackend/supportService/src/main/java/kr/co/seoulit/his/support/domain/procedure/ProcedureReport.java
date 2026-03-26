package kr.co.seoulit.his.support.domain.procedure;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * PROC(시술/내시경) 결과 리포트
 * - Order(PROC)에 귀속되는 결과물
 * - Support에서 작성 → Clinical Order 상태 RESULTED 전이
 */
@Entity
@Table(name = "procedure_report")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProcedureReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long procedureReportId;

    @Column(nullable = false)
    private Long orderId;

    private String reportText;

    @Column(nullable = false)
    private String status; // DONE (1차)

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
