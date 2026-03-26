package kr.co.seoulit.his.admin.domain.master.department;

import kr.co.seoulit.his.admin.domain.master.department.dto.DepartmentCreateRequest;
import kr.co.seoulit.his.admin.domain.master.department.dto.DepartmentResponse;
import kr.co.seoulit.his.admin.domain.master.department.dto.DepartmentUpdateRequest;
import kr.co.seoulit.his.admin.global.common.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SYS')")
@RequestMapping("/master/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService service;

    @GetMapping
    public List<DepartmentResponse> list() {
        return service.list();
    }

    // 조회 표준화(검색/페이징/정렬) - 기존과 호환을 위해 /search로 추가
    @GetMapping("/search")
    public PageResponse<DepartmentResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return service.search(keyword, active, pageable);
    }

    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/by-code/{code}")
    public DepartmentResponse getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @PostMapping
    public DepartmentResponse create(@RequestBody @Valid DepartmentCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable Long id, @RequestBody @Valid DepartmentUpdateRequest req) {
        return service.update(id, req);
    }

    // 물리삭제 대신 비활성화(현업형)
    @DeleteMapping("/{id}")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
