package kr.co.seoulit.his.support.domain.execution;

import jakarta.validation.Valid;
import kr.co.seoulit.his.support.domain.execution.dto.*;
import kr.co.seoulit.his.support.domain.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService service;

    // --------------------
    // INJECTION
    // --------------------

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping("/injections/tasks")
    public ResponseEntity<InjectionExecResponse> createInjectionTask(@Valid @RequestBody CreateInjectionTaskRequest req) {
        return ResponseEntity.ok(service.createInjectionTask(req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping("/injections/{id}/start")
    public ResponseEntity<InjectionExecResponse> startInjection(@PathVariable Long id) {
        return ResponseEntity.ok(service.startInjection(id));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping("/injections/{id}/complete")
    public ResponseEntity<InjectionExecResponse> completeInjection(@PathVariable Long id, @Valid @RequestBody CompleteInjectionRequest req) {
        return ResponseEntity.ok(service.completeInjection(id, req));
    }

    // [Compat] 기존 완료-기록 방식
    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping("/injections")
    public ResponseEntity<InjectionExecResponse> recordInjection(@Valid @RequestBody RecordInjectionRequest req) {
        return ResponseEntity.ok(service.recordInjection(req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @GetMapping("/injections")
    public ResponseEntity<List<InjectionExecResponse>> listInjections(@RequestParam(required = false) Long finalOrderId) {
        return ResponseEntity.ok(service.listInjections(finalOrderId));
    }

    // --------------------
    // MED (약제 조제/투약)
    // --------------------

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @PostMapping("/med-execs/tasks")
    public ResponseEntity<MedExecResponse> createMedExecTask(@Valid @RequestBody CreateMedExecTaskRequest req) {
        return ResponseEntity.ok(service.createMedExecTask(req));
    }

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @PostMapping("/med-execs/{id}/start")
    public ResponseEntity<MedExecResponse> startMedExec(@PathVariable Long id) {
        return ResponseEntity.ok(service.startMedExec(id));
    }

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @PostMapping("/med-execs/{id}/complete")
    public ResponseEntity<MedExecResponse> completeMedExec(@PathVariable Long id, @Valid @RequestBody CompleteMedExecRequest req) {
        return ResponseEntity.ok(service.completeMedExec(id, req));
    }

    @PreAuthorize("hasAnyRole('PHARM','SYS')")
    @GetMapping("/med-execs")
    public ResponseEntity<List<MedExecResponse>> listMedExecs(@RequestParam(required = false) Long finalOrderId) {
        return ResponseEntity.ok(service.listMedExecs(finalOrderId));
    }

    // --------------------
    // PROC (내시경) - 기존 완료-기록 방식 유지
    // --------------------

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping("/endoscopy-reports")
    public ResponseEntity<EndoscopyReportResponse> recordEndoscopy(@Valid @RequestBody RecordEndoscopyReportRequest req) {
        return ResponseEntity.ok(service.recordEndoscopy(req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @GetMapping("/endoscopy-reports")
    public ResponseEntity<List<EndoscopyReportResponse>> listEndoscopyReports(@RequestParam(required = false) Long finalOrderId) {
        return ResponseEntity.ok(service.listEndoscopyReports(finalOrderId));
    }
}
