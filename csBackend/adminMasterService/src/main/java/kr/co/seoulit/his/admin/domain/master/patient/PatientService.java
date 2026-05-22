package kr.co.seoulit.his.admin.domain.master.patient;

import kr.co.seoulit.his.admin.audit.MasterAuditClient;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientResolveRequest;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientResolveResponse;
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

    /**
     * [B-2] 이름+전화로 환자를 조회하고, 없으면 신규 생성한다.
     *
     * 프론트엔드의 patientId = Date.now() 임시 ID 발명 구조를 대체.
     * 신규 생성 시 patientId를 지정하지 않으므로 DB IDENTITY가 채번한다(V4 마이그레이션).
     */
    @Transactional
    public PatientResolveResponse resolveByNamePhone(PatientResolveRequest req) {
        String name = req.name().trim();
        String phone = req.phone() != null ? req.phone().trim() : null;

        // 이름+전화 일치 환자가 있으면 재사용 (전화가 비어 있으면 항상 신규 생성)
        if (phone != null && !phone.isBlank()) {
            Patient existing = patients.findFirstByNameAndPhoneOrderByPatientIdAsc(name, phone).orElse(null);
            if (existing != null) {
                return new PatientResolveResponse(existing.getPatientId(), existing.getName(), false);
            }
        }

        Patient p = Patient.builder()
                .name(name)
                .gender(req.gender())
                .rrnMasked(buildRrnMasked(req.rrnFront(), req.rrnBack()))
                .phone(phone)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Patient saved = patients.save(p); // patientId 미지정 → DB IDENTITY 채번
        audit.write("PATIENT_RESOLVE_CREATE", "PATIENT", String.valueOf(saved.getPatientId()), null,
                Map.of("name", saved.getName()));

        return new PatientResolveResponse(saved.getPatientId(), saved.getName(), true);
    }

    /** 화면용 주민번호 마스킹 — 프론트엔드 포맷("{front}-{back 첫자리}******")과 일치. */
    private static String buildRrnMasked(String rrnFront, String rrnBack) {
        String front = rrnFront != null ? rrnFront.trim() : "";
        String backFirst = (rrnBack != null && !rrnBack.isBlank()) ? rrnBack.trim().substring(0, 1) : "";
        return front + "-" + backFirst + "******";
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
