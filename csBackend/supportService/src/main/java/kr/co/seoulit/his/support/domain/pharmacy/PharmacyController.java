package kr.co.seoulit.his.support.domain.pharmacy;

import jakarta.validation.Valid;
import kr.co.seoulit.his.support.global.common.dto.ArchiveRequest;
import kr.co.seoulit.his.support.domain.pharmacy.dto.CreateDispenseRequest;
import kr.co.seoulit.his.support.domain.pharmacy.dto.DispenseResponse;
import kr.co.seoulit.his.support.domain.pharmacy.dto.UpdateDispenseRequest;
import kr.co.seoulit.his.support.domain.pharmacy.service.PharmacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dispenses")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyService service;

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @PostMapping
    public ResponseEntity<DispenseResponse> dispense(@Valid @RequestBody CreateDispenseRequest req) {
        return ResponseEntity.ok(service.dispense(req));
    }

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @GetMapping("/{id}")
    public ResponseEntity<DispenseResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id, false));
    }

    // ------------------ v3: 수정/아카이브(정책 기반) ------------------

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @GetMapping("/{id}/with-archived")
    public ResponseEntity<DispenseResponse> getIncludeArchived(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id, true));
    }

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @PutMapping("/{id}")
    public ResponseEntity<DispenseResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateDispenseRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<DispenseResponse> archive(@PathVariable Long id, @RequestBody(required = false) ArchiveRequest req) {
        String reason = (req == null ? null : req.reason());
        return ResponseEntity.ok(service.archive(id, reason));
    }
}
