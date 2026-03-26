package kr.co.seoulit.his.admin.domain.frontoffice.visit;

import jakarta.validation.Valid;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitResponse;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitStatusUpdateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService service;

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping
    public ResponseEntity<VisitResponse> create(@Valid @RequestBody VisitCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @GetMapping("/{id}")
    public ResponseEntity<VisitResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @GetMapping
    public ResponseEntity<List<VisitResponse>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.list(status));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PutMapping("/{id}")
    public ResponseEntity<VisitResponse> update(@PathVariable Long id, @RequestBody VisitUpdateRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    // 워크플로우: 상태 전이(허용 전이만)
    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping("/{id}/status")
    public ResponseEntity<VisitResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody VisitStatusUpdateRequest req) {
        return ResponseEntity.ok(service.updateStatus(id, req));
    }

    // 워크플로우: 접수 취소(정책)
    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<VisitResponse> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.cancel(id, reason));
    }
}
