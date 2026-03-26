package kr.co.seoulit.his.support.domain.worklist;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_worklist_task",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_support_worklist_task_order", columnNames = {"order_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorklistTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "worklist_task_id")
    private Long worklistTaskId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "category", nullable = false, length = 20)
    private String category;


    @Column(name = "primary_item_code", length = 50)
    private String primaryItemCode;

    @Column(name = "primary_item_name", length = 100)
    private String primaryItemName;


    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
