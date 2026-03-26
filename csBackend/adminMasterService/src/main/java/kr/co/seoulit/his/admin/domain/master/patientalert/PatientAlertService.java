package kr.co.seoulit.his.admin.domain.master.patientalert;

import kr.co.seoulit.his.admin.audit.MasterAuditClient;
import kr.co.seoulit.his.admin.domain.master.patientalert.dto.PatientAlertResponse;
import kr.co.seoulit.his.admin.domain.master.patientalert.dto.PatientAlertUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientAlertService {

    private final PatientAlertRepository alerts;
    private final MasterAuditClient audit;

    @Transactional
    public PatientAlertResponse upsert(PatientAlertUpsertRequest req) {
        PatientAlert a = null;
        if (req.patientAlertId() != null) {
            a = alerts.findById(req.patientAlertId()).orElse(null);
        }
        if (a == null) {
            a = PatientAlert.builder()
                    .createdAt(LocalDateTime.now())
                    .build();
        }
        a.setPatientId(req.patientId());
        a.setType(req.type());
        a.setMessage(req.message());
        a.setActive(req.active() != null ? req.active() : true);
        a.setUpdatedAt(LocalDateTime.now());

        PatientAlert saved = alerts.save(a);
        audit.write("PATIENT_ALERT_UPSERT", "PATIENT_ALERT", String.valueOf(saved.getPatientAlertId()), null,
                Map.of("patientId", String.valueOf(saved.getPatientId()), "type", saved.getType(), "active", String.valueOf(saved.getActive())));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PatientAlertResponse> listActiveByPatientIds(List<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) return List.of();
        return alerts.findByPatientIdInAndActive(patientIds, true).stream().map(this::toResponse).toList();
    }

    private PatientAlertResponse toResponse(PatientAlert a) {
        return new PatientAlertResponse(
                a.getPatientAlertId(),
                a.getPatientId(),
                a.getType(),
                a.getMessage(),
                a.getActive(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
