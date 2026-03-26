package kr.co.seoulit.his.clinical.domain.emr.soap;

import jakarta.validation.Valid;
import kr.co.seoulit.his.clinical.domain.emr.soap.dto.SoapArchiveRequest;
import kr.co.seoulit.his.clinical.domain.emr.soap.dto.SoapResponse;
import kr.co.seoulit.his.clinical.domain.emr.soap.dto.SoapUpsertRequest;
import kr.co.seoulit.his.clinical.domain.emr.soap.dto.SoapVersionResponse;
import kr.co.seoulit.his.clinical.domain.emr.soap.service.SoapNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/emr/soaps")
@RequiredArgsConstructor
public class SoapController {

    private final SoapNoteService service;

    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @PutMapping("/{visitId}")
    public ResponseEntity<SoapResponse> upsert(@PathVariable Long visitId, @Valid @RequestBody SoapUpsertRequest req) {
        return ResponseEntity.ok(service.upsert(visitId, req));
    }

    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @GetMapping("/{visitId}")
    public ResponseEntity<SoapResponse> get(@PathVariable Long visitId) {
        return ResponseEntity.ok(service.get(visitId));
    }

    // =========================
    // [ADDED] Version/History APIs
    // =========================
    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @GetMapping("/{visitId}/history")
    public ResponseEntity<List<SoapVersionResponse>> history(@PathVariable Long visitId) {
        return ResponseEntity.ok(service.history(visitId));
    }

    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @GetMapping("/{visitId}/versions/{versionNo}")
    public ResponseEntity<SoapResponse> version(@PathVariable Long visitId, @PathVariable Integer versionNo) {
        return ResponseEntity.ok(service.getVersion(visitId, versionNo));
    }

    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @PostMapping("/{visitId}/archive")
    public ResponseEntity<SoapResponse> archive(@PathVariable Long visitId, @Valid @RequestBody(required = false) SoapArchiveRequest req) {
        return ResponseEntity.ok(service.archive(visitId, req));
    }
}
