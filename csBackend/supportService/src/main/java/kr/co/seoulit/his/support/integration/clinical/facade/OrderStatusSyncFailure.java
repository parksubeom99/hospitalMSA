package kr.co.seoulit.his.support.integration.clinical.facade;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_sync_failure", indexes = {
        @Index(name = "idx_ossf_status_next_retry", columnList = "syncStatus,nextRetryAt"),
        @Index(name = "idx_ossf_order_status", columnList = "orderId,targetStatus")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusSyncFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 30)
    private String targetStatus; // IN_PROGRESS / RESULTED / DONE

    @Column(nullable = false, length = 255)
    private String endpointUrl;

    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 120)
    private String traceId;

    @Column(nullable = false, length = 20)
    private String syncStatus; // PENDING / SUCCESS / GAVE_UP

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private LocalDateTime nextRetryAt;

    private LocalDateTime lastTriedAt;
    private LocalDateTime succeededAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
