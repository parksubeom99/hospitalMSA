package kr.co.seoulit.his.admin.domain.master.patientalert;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_alert", indexes = {
        @Index(name = "idx_patient_alert_patient", columnList = "patientId"),
        @Index(name = "idx_patient_alert_active", columnList = "active")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PatientAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long patientAlertId;

    @Column(nullable = false)
    private Long patientId;

    /** ALLERGY/FALL_RISK/ISOLATION/CAUTION */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
