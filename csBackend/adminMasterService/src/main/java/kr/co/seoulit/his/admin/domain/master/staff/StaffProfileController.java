package kr.co.seoulit.his.admin.domain.master.staff;

import kr.co.seoulit.his.admin.domain.master.staff.dto.StaffResponse;
import kr.co.seoulit.his.admin.domain.master.staff.dto.StaffUpsertRequest;
import kr.co.seoulit.his.admin.domain.master.staff.dto.StaffUpdateRequest;
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
@RequestMapping("/master/staff")
@RequiredArgsConstructor
public class StaffProfileController {

    private final StaffProfileService service;

    @GetMapping
    public List<StaffResponse> list() {
        return service.list();
    }

    // 조회 표준화(검색/페이징/정렬) - 기존과 호환을 위해 /search로 추가
    @GetMapping("/search")
    public PageResponse<StaffResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "loginId", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return service.search(keyword, jobType, departmentId, active, pageable);
    }

    @GetMapping("/{id}")
    public StaffResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/by-login/{loginId}")
    public StaffResponse getByLoginId(@PathVariable String loginId) {
        return service.getByLoginId(loginId);
    }

    @PostMapping
    public StaffResponse upsert(@RequestBody @Valid StaffUpsertRequest req) {
        return service.upsert(req);
    }

    @PutMapping("/{id}")
    public StaffResponse update(@PathVariable Long id, @RequestBody @Valid StaffUpdateRequest req) {
        return service.update(id, req);
    }

    // 물리삭제 대신 비활성화(현업형)
    @DeleteMapping("/{id}")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
