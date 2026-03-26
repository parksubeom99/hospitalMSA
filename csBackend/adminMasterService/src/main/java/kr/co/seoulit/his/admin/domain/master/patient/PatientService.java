package kr.co.seoulit.his.admin.domain.master.patient;

import kr.co.seoulit.his.admin.audit.MasterAuditClient;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientResponse;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patients;
    private final MasterAuditClient audit;

    @Transactional
    public PatientResponse upsert(PatientUpsertRequest req) {
        Patient p = patients.findById(req.patientId()).orElse(null);
        if (p == null) {
            p = Patient.builder()
                    .patientId(req.patientId())
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        p.setName(req.name());
        p.setGender(req.gender());
        p.setRrnMasked(req.rrnMasked());
        p.setBirthDate(req.birthDate());
        p.setPhone(req.phone());
        p.setActive(req.active() != null ? req.active() : true);
        p.setUpdatedAt(LocalDateTime.now());

        Patient saved = patients.save(p);
        audit.write("PATIENT_UPSERT", "PATIENT", String.valueOf(saved.getPatientId()), null,
                Map.of("name", saved.getName(), "active", String.valueOf(saved.getActive())));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PatientResponse get(Long id) {
        return patients.findById(id).map(this::toResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> listByIds(List<Long> ids) {
        return patients.findAllById(ids).stream().map(this::toResponse).toList();
    }

    private PatientResponse toResponse(Patient p) {
        return new PatientResponse(
                p.getPatientId(),
                p.getName(),
                p.getGender(),
                p.getRrnMasked(),
                p.getBirthDate(),
                p.getPhone(),
                p.getActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
