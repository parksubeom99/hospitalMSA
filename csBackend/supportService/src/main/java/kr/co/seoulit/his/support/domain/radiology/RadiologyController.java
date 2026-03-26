package kr.co.seoulit.his.support.domain.radiology;

import jakarta.validation.Valid;
import kr.co.seoulit.his.support.domain.radiology.dto.CreateRadiologyReportRequest;
import kr.co.seoulit.his.support.domain.radiology.dto.RadiologyReportResponse;
import kr.co.seoulit.his.support.domain.radiology.dto.UpdateRadiologyReportRequest;
import kr.co.seoulit.his.support.integration.clinical.client.OrderClient;
import kr.co.seoulit.his.support.domain.worklist.service.WorklistService;
import kr.co.seoulit.his.support.global.common.dto.ArchiveRequest;
import kr.co.seoulit.his.support.domain.radiology.service.RadiologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/radiology-reports")
@RequiredArgsConstructor
public class RadiologyController {

    private final RadiologyService service;
    private final OrderClient orderClient;
    private final WorklistService worklists;

    @PreAuthorize("hasAnyRole('RAD','SYS')")
    @PostMapping
    public ResponseEntity<RadiologyReportResponse> record(@Valid @RequestBody CreateRadiologyReportRequest req) {
        RadiologyReportResponse res = service.create(req);
        // ✅ 결과 등록 후 Clinical(Order) 상태를 RESULTED로 자동 전환
        orderClient.markResulted(req.orderId());
        worklists.completeWork(req.orderId(), "RAD");
        return ResponseEntity.ok(res);
    }

    
/**
 * RAD 리포트 목록 조회
 * 예) GET /radiology-reports?orderId=123
 */
@PreAuthorize("hasAnyRole('RAD','DOC','NUR','SYS')")
@GetMapping
public ResponseEntity<java.util.List<RadiologyReportResponse>> list(@RequestParam(required = false) Long orderId) {
    return ResponseEntity.ok(service.list(orderId));
}

    @PreAuthorize("hasAnyRole('RAD','SYS')")
    @GetMapping("/{id}")
    public ResponseEntity<RadiologyReportResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id, false));
    }

    @PreAuthorize("hasAnyRole('RAD','SYS')")
    @GetMapping("/{id}/with-archived")
    public ResponseEntity<RadiologyReportResponse> getIncludeArchived(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id, true));
    }

    @PreAuthorize("hasAnyRole('RAD','SYS')")
    @PutMapping("/{id}")
    public ResponseEntity<RadiologyReportResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateRadiologyReportRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PreAuthorize("hasAnyRole('RAD','SYS')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<RadiologyReportResponse> archive(@PathVariable Long id, @RequestBody(required = false) ArchiveRequest req) {
        String reason = (req == null ? null : req.reason());
        return ResponseEntity.ok(service.archive(id, reason));
    }
}