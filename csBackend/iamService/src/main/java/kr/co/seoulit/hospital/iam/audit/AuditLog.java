package kr.co.seoulit.hospital.iam.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="audit_log",
        indexes = {
                @Index(name="idx_audit_created_at", columnList = "createdAt"),
                @Index(name="idx_audit_actor", columnList = "actorLoginId"),
                @Index(name="idx_audit_action", columnList = "action"),
                @Index(name="idx_audit_service", columnList = "serviceName"),
                @Index(name="idx_audit_result", columnList = "result")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @Column(length=40)
    private String eventId; // UUID/ULID

    @Column(nullable=false)
    private String actorLoginId;

    @Column(nullable=false)
    private String serviceName; // IAM/MASTER/ADMIN/CLINICAL/SUPPORT

    @Column(nullable=false)
    private String action; // e.g., LOGIN_SUCCESS, ORDER_CREATED

    @Column(nullable=false)
    private String result; // SUCCESS/FAIL

    private String targetType; // VISIT/ORDER/PATIENT/RESULT/AUTH
    private String targetId;
    private Long patientId;

    private String ipAddress;
    private String userAgent;

    @Column(nullable=false)
    private LocalDateTime createdAt;

    @Column(nullable=false)
    private boolean archived;

    private LocalDateTime archivedAt;

    @Column(columnDefinition = "TEXT")
    private String detailJson;
}
