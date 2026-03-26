package kr.co.seoulit.his.support.domain.procedure;

import jakarta.validation.Valid;
import kr.co.seoulit.his.support.integration.clinical.client.OrderClient;
import kr.co.seoulit.his.support.domain.worklist.service.WorklistService;
import kr.co.seoulit.his.support.domain.procedure.dto.CreateProcedureReportRequest;
import kr.co.seoulit.his.support.domain.procedure.dto.ProcedureReportResponse;
import kr.co.seoulit.his.support.domain.procedure.dto.UpdateProcedureReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/procedure-reports")
@RequiredArgsConstructor
@Slf4j
public class ProcedureReportController {

    private final ProcedureReportService service;
    private final OrderClient orderClient;
    private final WorklistService worklists;

    /**
     * PROC(시술/내시경) 결과 등록
     * - 저장 성공 후 Clinical /orders/{id}/resulted 자동 전이 시도
     */
    @PreAuthorize("hasAnyRole('PROC','SYS')")
    @PostMapping
    public ResponseEntity<ProcedureReportResponse> create(@Valid @RequestBody CreateProcedureReportRequest req) {
        ProcedureReportResponse saved = service.create(req);

        // 결과 저장은 반드시 성공해야 하고, 상태전이는 실패해도 결과는 유지되게(Degrade)
        try {
            orderClient.markResulted(req.orderId());
            worklists.completeWork(req.orderId(), "PROC");
        } catch (Exception e) {
            log.warn("Failed to mark order RESULTED. orderId={}, reason={}", req.orderId(), e.getMessage());
        }

        return ResponseEntity.ok(saved);
    }

    /**
     * PROC 결과 목록 조회
     * 예) GET /procedure-reports?orderId=123
     */
    @PreAuthorize("hasAnyRole('DOC','NUR','PROC','SYS')")
    @GetMapping
    public ResponseEntity<List<ProcedureReportResponse>> list(@RequestParam(required = false) Long orderId) {
        return ResponseEntity.ok(service.list(orderId));
    }

    /**
     * PROC 결과 단건 조회
     * 예) GET /procedure-reports/{id}
     */
    @PreAuthorize("hasAnyRole('DOC','NUR','PROC','SYS')")
    @GetMapping("/{id}")
    public ResponseEntity<ProcedureReportResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    /**
     * PROC 결과 수정(최소 수정: reportText)
     * 예) PUT /procedure-reports/{id}
     */
    @PreAuthorize("hasAnyRole('PROC','SYS')")
    @PutMapping("/{id}")
    public ResponseEntity<ProcedureReportResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateProcedureReportRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }
}
