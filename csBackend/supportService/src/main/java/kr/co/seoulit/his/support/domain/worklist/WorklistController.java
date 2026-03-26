package kr.co.seoulit.his.support.domain.worklist;

import kr.co.seoulit.his.support.domain.worklist.dto.WorkItemDto;
import kr.co.seoulit.his.support.domain.worklist.dto.CreateWorklistTaskRequest;
import kr.co.seoulit.his.support.domain.worklist.service.WorklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// ✅ 프론트/기존 클라이언트가 /worklists 를 쓰는 경우가 있어, 2개 경로를 동시에 허용합니다.
@RequestMapping({"/worklist", "/worklists"})
@RequiredArgsConstructor
public class WorklistController {

    private final WorklistService service;

    /**
     * v6: Clinical(Order 생성) -> Support(Worklist 자동 생성) 고정
     * - 내부 연동(REST) 기반으로 먼저 완성하고, 추후 Outbox/Kafka로 교체 가능
     * - 동일 orderId로 반복 호출되어도 upsert로 멱등 처리
     */
    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping("/create")
    public ResponseEntity<WorkItemDto> create(@Valid @RequestBody CreateWorklistTaskRequest req) {
        var t = service.upsertTask(req.orderId(), req.visitId(), req.category(), req.status(), req.primaryItemCode(), req.primaryItemName());
        return ResponseEntity.ok(new WorkItemDto(t.getOrderId(), t.getVisitId(), t.getCategory(), t.getStatus(), t.getPrimaryItemCode(), t.getPrimaryItemName()));
    }

    /**
     * Worklist 조회
     * - status 미지정 시 기본값은 NEW 입니다.
     * 예) /worklist?category=LAB
     *     /worklist?category=LAB&status=IN_PROGRESS
     */
    // v5: PROC(시술/내시경) 카테고리 지원
    @PreAuthorize("hasAnyRole('NUR','LAB','RAD','PHARM','PROC','SYS')")
    @GetMapping
    public ResponseEntity<List<WorkItemDto>> getWorklist(
            @RequestParam String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String primaryItemCode
    ) {
        return ResponseEntity.ok(service.getWorklist(category, status, primaryItemCode));
    }

    /**
     * 담당자 착수(진행중 전환)
     * - 오더 상태를 IN_PROGRESS로 변경합니다.
     * 예) POST /worklist/{orderId}/start
     */
    @PreAuthorize("hasAnyRole('LAB','RAD','PHARM','PROC','SYS')")
    @PostMapping("/{orderId}/start")
    public ResponseEntity<Void> start(
            @PathVariable Long orderId,
            @RequestParam(required = false) String category
    ) {
        service.startWork(orderId, category);
        return ResponseEntity.ok().build();
    }
}
