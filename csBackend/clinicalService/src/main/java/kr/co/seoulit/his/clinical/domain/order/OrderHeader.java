package kr.co.seoulit.his.clinical.domain.order;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="order_header")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderHeader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable=false)
    private Long visitId;

    @Column(nullable=false)
    private String category; // LAB/RAD/PHARM

    @Column(nullable=false)
    private String status; // NEW, IN_PROGRESS, DONE(결과입력완료), REVIEWED(의사확인), CANCELED


    @Column(name = "primary_item_code", length = 50)
    private String primaryItemCode;

    @Column(name = "primary_item_name", length = 100)
    private String primaryItemName;


    @Column(unique = true)
    private String idempotencyKey; // 중복 발행 방지(1차)

    @Column(nullable=false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // =========================
    // [ADDED] Soft Delete fields
    // =========================
    @Builder.Default
    @Column(nullable=false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @Column(length = 100)
    private String deletedBy; // loginId

    @Column(length = 255)
    private String deletedReason;
}
