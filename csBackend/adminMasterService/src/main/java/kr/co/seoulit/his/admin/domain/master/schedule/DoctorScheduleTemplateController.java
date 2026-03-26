package kr.co.seoulit.his.admin.domain.master.schedule;

import kr.co.seoulit.his.admin.global.common.page.PageResponse;
import kr.co.seoulit.his.admin.domain.master.schedule.dto.ScheduleTemplateCreateRequest;
import kr.co.seoulit.his.admin.domain.master.schedule.dto.ScheduleTemplateResponse;
import kr.co.seoulit.his.admin.domain.master.schedule.dto.ScheduleTemplateUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/master/schedules/templates")
@RequiredArgsConstructor
public class DoctorScheduleTemplateController {

    private final DoctorScheduleTemplateService service;

    @PostMapping
    public ScheduleTemplateResponse create(@RequestBody @Valid ScheduleTemplateCreateRequest req) {
        return service.create(req);
    }

    // 단순 조회(활성 템플릿)
    @GetMapping
    public List<ScheduleTemplateResponse> listActive(@RequestParam Long staffProfileId) {
        return service.listActive(staffProfileId);
    }

    // 조회 표준화(검색/페이징/정렬)
    @GetMapping("/search")
    public PageResponse<ScheduleTemplateResponse> search(
            @RequestParam Long staffProfileId,
            @PageableDefault(size = 20, sort = "dayOfWeek", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return service.search(staffProfileId, pageable);
    }

    @PutMapping("/{id}")
    public ScheduleTemplateResponse update(@PathVariable Long id, @RequestBody @Valid ScheduleTemplateUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
