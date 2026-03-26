package kr.co.seoulit.his.support.domain.execution;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="endoscopy_report",
        indexes = {
                @Index(name="idx_endo_final_order", columnList = "final_order_id"),
                @Index(name="idx_endo_idem", columnList = "idempotency_key", unique = true)
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EndoscopyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long endoscopyReportId;

    @Column(nullable = false)
    private Long finalOrderId;

    @Column(nullable = false, length = 32)
    private String status; // DONE

    @Column(nullable = false, length = 2000)
    private String reportText;

    @Column(nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
