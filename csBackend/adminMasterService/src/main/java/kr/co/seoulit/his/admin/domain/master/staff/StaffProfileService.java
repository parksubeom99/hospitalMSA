package kr.co.seoulit.his.admin.domain.master.staff;

import kr.co.seoulit.his.admin.audit.MasterAuditClient;
import kr.co.seoulit.his.admin.global.common.page.PageResponse;
import kr.co.seoulit.his.admin.domain.master.staff.dto.StaffResponse;
import kr.co.seoulit.his.admin.domain.master.staff.dto.StaffUpsertRequest;
import kr.co.seoulit.his.admin.domain.master.staff.dto.StaffUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap; // [ADDED]
import java.util.List;
import java.util.Locale;
import java.util.Map;    // [ADDED]

@Service
@RequiredArgsConstructor
public class StaffProfileService {

    private final StaffProfileRepository repo;
    private final MasterAuditClient auditClient;

    @Transactional(readOnly = true)
    public List<StaffResponse> list() {
        // [MODIFIED] findAll() → findAll(Sort) — staffProfileId 오름차순 정렬
        // Oracle은 정렬 없이 조회 시 내부 저장 순서 반환 → ID 순서 보장 안됨
        return repo.findAll(Sort.by(Sort.Direction.ASC, "staffProfileId"))
                .stream().map(this::toResponse).toList();
    }

    // 조회 표준화(검색/페이징/정렬)
    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> search(String keyword, String jobType, Long departmentId, Boolean active, Pageable pageable) {
        Specification<StaffProfile> spec = Specification
                .where(StaffSpecifications.keyword(keyword))
                .and(StaffSpecifications.jobType(jobType))
                .and(StaffSpecifications.departmentId(departmentId))
                .and(StaffSpecifications.active(active));
        return PageResponse.from(repo.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Long id) {
        StaffProfile s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("StaffProfile not found: " + id));
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public StaffResponse getByLoginId(String loginId) {
        StaffProfile s = repo.findByLoginId(loginId).orElseThrow(() -> new IllegalArgumentException("StaffProfile not found by loginId: " + loginId));
        return toResponse(s);
    }

    @Transactional
    public StaffResponse upsert(StaffUpsertRequest req) {
        StaffProfile s = repo.findByLoginId(req.loginId()).orElseGet(StaffProfile::new);
        s.setLoginId(req.loginId());
        s.setName(req.name());
        s.setJobType(req.jobType());
        s.setDepartmentId(req.departmentId());
        s.setPhone(normalizePhone(req.phone()));
        s.setEmail(normalizeEmail(req.email()));
        if (req.active() != null) {
            s.setActive(req.active());
        } else if (s.getStaffProfileId() == null) {
            s.setActive(true);
        }

        StaffProfile saved = repo.save(s);

        // [MODIFIED] Map.of() → new HashMap<>()
        // Map.of()는 null 값을 허용하지 않아 departmentId/phone/email이 null인
        // 원무직원 저장 시 NullPointerException 발생 → 500 에러 원인
        // HashMap은 null 값 허용 → 원무직원 포함 모든 직원 타입 안전 처리
        Map<String, Object> detail = new HashMap<>();
        detail.put("loginId", saved.getLoginId());
        detail.put("name", saved.getName());
        detail.put("jobType", saved.getJobType());
        detail.put("departmentId", saved.getDepartmentId());
        detail.put("phone", saved.getPhone());
        detail.put("email", saved.getEmail());
        detail.put("active", saved.isActive());
        auditClient.write("STAFF_UPSERTED", "STAFF_PROFILE", String.valueOf(saved.getStaffProfileId()), null, detail);

        return toResponse(saved);
    }

    @Transactional
    public StaffResponse update(Long id, StaffUpdateRequest req) {
        StaffProfile s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("StaffProfile not found: " + id));
        s.setName(req.name());
        s.setJobType(req.jobType());
        s.setDepartmentId(req.departmentId());
        s.setPhone(normalizePhone(req.phone()));
        s.setEmail(normalizeEmail(req.email()));
        if (req.active() != null) s.setActive(req.active());

        StaffProfile saved = repo.save(s);

        // [MODIFIED] Map.of() → new HashMap<>()
        // 원무직원: departmentId=null, phone/email=null 가능 → Map.of() NPE → 500
        // HashMap으로 교체하여 null 값 허용
        Map<String, Object> detail = new HashMap<>();
        detail.put("loginId", saved.getLoginId());
        detail.put("name", saved.getName());
        detail.put("jobType", saved.getJobType());
        detail.put("departmentId", saved.getDepartmentId());
        detail.put("phone", saved.getPhone());
        detail.put("email", saved.getEmail());
        detail.put("active", saved.isActive());
        auditClient.write("STAFF_UPDATED", "STAFF_PROFILE", String.valueOf(saved.getStaffProfileId()), null, detail);

        return toResponse(saved);
    }

    // 물리삭제 대신 비활성화(현업형)
    @Transactional
    public void deactivate(Long id) {
        StaffProfile s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("StaffProfile not found: " + id));
        s.setActive(false);
        repo.save(s);
        // [MODIFIED] Map.of() → new HashMap<>() (loginId가 null일 경우 대비, 일관성 유지)
        Map<String, Object> detail = new HashMap<>();
        detail.put("loginId", s.getLoginId());
        auditClient.write("STAFF_DEACTIVATED", "STAFF_PROFILE", String.valueOf(s.getStaffProfileId()), null, detail);
    }

    private StaffResponse toResponse(StaffProfile s) {
        return new StaffResponse(
                s.getStaffProfileId(),
                s.getLoginId(),
                s.getName(),
                s.getJobType(),
                s.getDepartmentId(),
                s.getPhone(),
                s.getEmail(),
                s.isActive()
        );
    }

    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) return null;
        if (digits.length() == 11 && digits.startsWith("010")) {
            return digits.substring(0,3) + "-" + digits.substring(3,7) + "-" + digits.substring(7,11);
        }
        return raw.trim();
    }

    private String normalizeEmail(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return v.isBlank() ? null : v;
    }
}