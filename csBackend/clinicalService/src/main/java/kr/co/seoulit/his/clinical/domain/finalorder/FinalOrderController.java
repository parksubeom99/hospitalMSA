package kr.co.seoulit.his.clinical.domain.finalorder;

import jakarta.validation.Valid;
import kr.co.seoulit.his.clinical.domain.finalorder.dto.CreateFinalOrderRequest;
import kr.co.seoulit.his.clinical.domain.finalorder.dto.FinalOrderResponse;
import kr.co.seoulit.his.clinical.domain.finalorder.dto.UpdateFinalOrderStatusRequest;
import kr.co.seoulit.his.clinical.domain.finalorder.service.FinalOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/final-orders")
@RequiredArgsConstructor
public class FinalOrderController {

    private final FinalOrderService service;

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping
    public ResponseEntity<FinalOrderResponse> create(@Valid @RequestBody CreateFinalOrderRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS','ADMIN','LAB','RAD','PHARM')")
    @PostMapping("/{id}/status")
    public ResponseEntity<FinalOrderResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateFinalOrderStatusRequest req) {
        return ResponseEntity.ok(service.updateStatus(id, req.getStatus()));
    }

    // =========================
    // [ADDED][STEP4] Finalize API
    // =========================


    // Plan→Execute 확정(Finalize)
    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @PostMapping("/{id}/finalize")
    public ResponseEntity<FinalOrderResponse> finalize(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizeOrder(id));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS','ADMIN','LAB','RAD','PHARM')")
    @GetMapping
    public ResponseEntity<List<FinalOrderResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(service.list(status, type));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS','ADMIN','LAB','RAD','PHARM')")
    @GetMapping("/{id}")
    public ResponseEntity<FinalOrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }
}
