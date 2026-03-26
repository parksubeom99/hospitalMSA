package kr.co.seoulit.his.admin.domain.frontoffice.appointment;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_appointment", indexes = {
        @Index(name = "idx_admin_appt_scheduled", columnList = "scheduledAt"),
        @Index(name = "idx_admin_appt_status", columnList = "status"),
        @Index(name = "idx_admin_appt_patient", columnList = "patientId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long appointmentId;

    private Long patientId;
    private String patientName;

    private String departmentCode;
    private String doctorId;

    /** BOOKED/ARRIVED/CANCELED */
    private String status;

    /** 예약일시 */
    private LocalDateTime scheduledAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
