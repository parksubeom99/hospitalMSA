package kr.co.seoulit.his.admin.domain.frontoffice.queue;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_queue_ticket", indexes = {
        @Index(name = "idx_admin_queue_status", columnList = "status"),
        @Index(name = "idx_admin_queue_visit", columnList = "visitId"),
        @Index(name = "idx_admin_queue_category", columnList = "category")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    private Long visitId;

    /** 대기열 카테고리(진료과/검사종류/창구 등). 예: IM, LAB, RAD, PHARM, FRONT */
    private String category;

    private String ticketNo;

    /** WAITING/CALLED/IN_PROGRESS/DONE */
    private String status;

    private LocalDateTime issuedAt;
    private LocalDateTime calledAt;
    private LocalDateTime doneAt;

    /** 마지막 반영한 이벤트 ID(중복 이벤트 방지용 - 최소 형태) */
    private String lastEventId;
}
