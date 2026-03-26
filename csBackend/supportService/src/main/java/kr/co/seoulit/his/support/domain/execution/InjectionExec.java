package kr.co.seoulit.his.support.domain.execution;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="injection_exec",
        indexes = {
                @Index(name="idx_inj_final_order", columnList = "final_order_id"),
                @Index(name="idx_inj_idem", columnList = "idempotency_key", unique = true)
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class InjectionExec {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long injectionExecId;

    @Column(nullable = false)
    private Long finalOrderId;

    @Column(nullable = false, length = 32)
    private String status; // DONE

    @Column(length = 400)
    private String note;

    @Column(nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
