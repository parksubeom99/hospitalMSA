package kr.co.seoulit.his.admin.domain.frontoffice.reservation;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_reservation", indexes = {
        @Index(name = "idx_admin_resv_status", columnList = "status"),
        @Index(name = "idx_admin_resv_scheduled", columnList = "scheduledAt"),
        @Index(name = "idx_admin_resv_patient", columnList = "patientId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    private Long patientId;
    private String patientName;

    private String departmentCode;
    private String doctorId;

    private LocalDateTime scheduledAt;

    /** BOOKED/CANCELED/CHECKED_IN */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDateTime canceledAt;
    private String cancelReason;

    /** 체크인 시 생성된 visitId */
    private Long visitId;
}
