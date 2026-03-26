package kr.co.seoulit.his.admin.domain.master.department;

import kr.co.seoulit.his.admin.audit.MasterAuditClient;
import kr.co.seoulit.his.admin.global.common.page.PageResponse;
import kr.co.seoulit.his.admin.domain.master.department.dto.DepartmentCreateRequest;
import kr.co.seoulit.his.admin.domain.master.department.dto.DepartmentResponse;
import kr.co.seoulit.his.admin.domain.master.department.dto.DepartmentUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository repo;
    private final MasterAuditClient auditClient;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    // 조회 표준화(검색/페이징/정렬)
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> search(String keyword, Boolean active, Pageable pageable) {
        Specification<Department> spec = Specification
                .where(DepartmentSpecifications.keyword(keyword))
                .and(DepartmentSpecifications.active(active));
        return PageResponse.from(repo.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) {
        Department d = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
        return toResponse(d);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getByCode(String code) {
        Department d = repo.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Department not found by code: " + code));
        return toResponse(d);
    }

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest req) {
        repo.findByCode(req.code()).ifPresent(d -> {
            throw new IllegalArgumentException("Department code already exists: " + req.code());
        });

        Department saved = repo.save(Department.builder()
                .code(req.code())
                .name(req.name())
                .active(true)
                .build());

        auditClient.write("DEPARTMENT_CREATED", "DEPARTMENT", String.valueOf(saved.getDepartmentId()), null,
                java.util.Map.of("code", saved.getCode(), "name", saved.getName()));

        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentUpdateRequest req) {
        Department d = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
        d.setName(req.name());
        if (req.active() != null) {
            d.setActive(req.active());
        }

        auditClient.write("DEPARTMENT_UPDATED", "DEPARTMENT", String.valueOf(d.getDepartmentId()), null,
                java.util.Map.of("name", d.getName(), "active", d.isActive()));

        return toResponse(d);
    }

    // 물리삭제 대신 비활성화(현업형)
    @Transactional
    public void deactivate(Long id) {
        Department d = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
        d.setActive(false);
        repo.save(d);
        auditClient.write("DEPARTMENT_DEACTIVATED", "DEPARTMENT", String.valueOf(d.getDepartmentId()), null,
                java.util.Map.of("code", d.getCode()));
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getDepartmentId(), d.getCode(), d.getName(), d.isActive());
    }
}
