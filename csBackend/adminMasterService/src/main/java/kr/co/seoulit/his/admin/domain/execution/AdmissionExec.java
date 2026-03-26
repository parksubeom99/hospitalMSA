package kr.co.seoulit.his.admin.domain.execution;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="admission_exec",
        indexes = {
                @Index(name="idx_adm_final_order", columnList = "final_order_id"),
                @Index(name="idx_adm_idem", columnList = "idempotency_key", unique = true)
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AdmissionExec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long admissionExecId;

    @Column(nullable = false)
    private Long finalOrderId;

    @Column(nullable = false, length = 64)
    private String ward;

    @Column(nullable = false, length = 32)
        private String status; // NEW/IN_PROGRESS/DONE

    @Column(nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    // [ADDED][STEP6] ADT lifecycle timestamps
    private LocalDateTime admittedAt;
    private LocalDateTime dischargedAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
