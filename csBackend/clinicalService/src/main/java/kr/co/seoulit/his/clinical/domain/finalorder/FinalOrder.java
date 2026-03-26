package kr.co.seoulit.his.clinical.domain.finalorder;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "final_order",
        indexes = {
                @Index(name="idx_final_order_visit", columnList = "visit_id"),
                @Index(name="idx_final_order_status", columnList = "status"),
                @Index(name="idx_final_order_idem", columnList = "idempotency_key", unique = true)
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FinalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long finalOrderId;

    @Column(nullable = false)
    private Long visitId;

    @Column(nullable = false, length = 32)
    private String type; // MED / INJECTION / ADMISSION / SURGERY ...

    @Column(nullable = false, length = 32)
    private String status; // ORDERED / IN_PROGRESS / DONE / CANCELED

    @Column(length = 400)
    private String note;

    @Column(nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
