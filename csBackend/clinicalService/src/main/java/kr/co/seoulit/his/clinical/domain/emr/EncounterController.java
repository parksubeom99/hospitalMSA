package kr.co.seoulit.his.clinical.domain.emr;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterArchiveRequest;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterCreateRequest;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterResponse;
import kr.co.seoulit.his.clinical.domain.emr.dto.EncounterUpdateRequest;
import kr.co.seoulit.his.clinical.domain.emr.service.EncounterNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterNoteService service;

    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @PostMapping
    public ResponseEntity<EncounterResponse> create(@Valid @RequestBody EncounterCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @GetMapping
    public ResponseEntity<List<EncounterResponse>> listByVisit(
            @RequestParam @NotNull Long visitId,
            @RequestParam(required = false, defaultValue = "false") boolean includeArchived
    ) {
        return ResponseEntity.ok(service.listByVisit(visitId, includeArchived));
    }

    // NOTE: 기존 호환 엔드포인트 유지 (예: /encounters/{visitId}/notes)
    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @GetMapping("/{visitId}/notes")
    public ResponseEntity<List<EncounterResponse>> listByVisitCompat(@PathVariable Long visitId) {
        return ResponseEntity.ok(service.listByVisit(visitId, false));
    }

    // =========================
    // [ADDED] Update / Archive APIs
    // =========================
    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @PutMapping("/{noteId}")
    public ResponseEntity<EncounterResponse> update(@PathVariable Long noteId, @Valid @RequestBody EncounterUpdateRequest req) {
        return ResponseEntity.ok(service.update(noteId, req));
    }

    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @PostMapping("/{noteId}/archive")
    public ResponseEntity<EncounterResponse> archive(@PathVariable Long noteId, @Valid @RequestBody(required = false) EncounterArchiveRequest req) {
        return ResponseEntity.ok(service.archive(noteId, req));
    }
}
