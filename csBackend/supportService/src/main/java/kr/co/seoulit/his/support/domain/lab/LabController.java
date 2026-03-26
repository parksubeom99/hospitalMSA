package kr.co.seoulit.his.support.domain.lab;

import jakarta.validation.Valid;
import kr.co.seoulit.his.support.domain.lab.dto.CreateLabResultRequest;
import kr.co.seoulit.his.support.domain.lab.dto.LabResultResponse;
import kr.co.seoulit.his.support.domain.lab.dto.UpdateLabResultRequest;
import kr.co.seoulit.his.support.integration.clinical.client.OrderClient;
import kr.co.seoulit.his.support.domain.worklist.service.WorklistService;
import kr.co.seoulit.his.support.global.common.dto.ArchiveRequest;
import kr.co.seoulit.his.support.domain.lab.service.LabResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/lab-results")
@RequiredArgsConstructor
public class LabController {

    private final LabResultService service;
    private final OrderClient orderClient;
    private final WorklistService worklists;

    @PreAuthorize("hasAnyRole('LAB','SYS')")
    @PostMapping
    public ResponseEntity<LabResultResponse> record(@Valid @RequestBody CreateLabResultRequest req) {
        LabResultResponse res = service.create(req);
        // ✅ 결과 등록 후 Clinical(Order) 상태를 RESULTED로 자동 전환
        orderClient.markResulted(req.orderId());
        worklists.completeWork(req.orderId(), "LAB");
        return ResponseEntity.ok(res);
    }

    
/**
 * LAB 결과 목록 조회
 * 예) GET /lab-results?orderId=123
 */
@PreAuthorize("hasAnyRole('LAB','DOC','NUR','SYS')")
@GetMapping
public ResponseEntity<java.util.List<LabResultResponse>> list(@RequestParam(required = false) Long orderId) {
    return ResponseEntity.ok(service.list(orderId));
}

    @PreAuthorize("hasAnyRole('LAB','SYS')")
    @GetMapping("/{id}")
    public ResponseEntity<LabResultResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id, false));
    }

    @PreAuthorize("hasAnyRole('LAB','SYS')")
    @GetMapping("/{id}/with-archived")
    public ResponseEntity<LabResultResponse> getIncludeArchived(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id, true));
    }

    @PreAuthorize("hasAnyRole('LAB','SYS')")
    @PutMapping("/{id}")
    public ResponseEntity<LabResultResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateLabResultRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PreAuthorize("hasAnyRole('LAB','SYS')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<LabResultResponse> archive(@PathVariable Long id, @RequestBody(required = false) ArchiveRequest req) {
        String reason = (req == null ? null : req.reason());
        return ResponseEntity.ok(service.archive(id, reason));
    }
}