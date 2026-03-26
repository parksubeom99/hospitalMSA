package kr.co.seoulit.his.admin.domain.execution;

import jakarta.validation.Valid;
import kr.co.seoulit.his.admin.domain.execution.dto.*;
import kr.co.seoulit.his.admin.domain.execution.service.AdminExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminExecutionController {

    private final AdminExecutionService service;

    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/admissions")
    public ResponseEntity<AdmissionExecResponse> admit(@Valid @RequestBody RecordAdmissionRequest req) {
        return ResponseEntity.ok(service.admit(req)); // compat: record as DONE
    }

    // =========================
    // [ADDED][STEP6] ADT execution lifecycle
    // =========================

    /** Finalize 이후, ADT 실행 작업 생성(NEW) */
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/admissions/tasks")
    public ResponseEntity<AdmissionExecResponse> createAdmissionTask(@Valid @RequestBody RecordAdmissionRequest req) {
        return ResponseEntity.ok(service.createAdmissionTask(req));
    }

    /** 입원(admit) = NEW -> IN_PROGRESS */
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/admissions/{admissionExecId}/admit")
    public ResponseEntity<AdmissionExecResponse> startAdmission(@PathVariable Long admissionExecId,
                                                                @Valid @RequestBody AdmitAdmissionRequest req) {
        return ResponseEntity.ok(service.startAdmission(admissionExecId, req));
    }

    /** 퇴원(discharge) = IN_PROGRESS -> DONE */
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/admissions/{admissionExecId}/discharge")
    public ResponseEntity<AdmissionExecResponse> discharge(@PathVariable Long admissionExecId) {
        return ResponseEntity.ok(service.discharge(admissionExecId));
    }
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @GetMapping("/admissions")
    public ResponseEntity<List<AdmissionExecResponse>> listAdmissions(@RequestParam(required = false) Long finalOrderId) {
        return ResponseEntity.ok(service.listAdmissions(finalOrderId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/surgeries")
    public ResponseEntity<SurgeryExecResponse> surgery(@Valid @RequestBody RecordSurgeryRequest req) {
        return ResponseEntity.ok(service.recordSurgery(req));
    }

    // =========================
    // [ADDED][STEP7] Surgery execution lifecycle
    // =========================

    /** Finalize 이후, 수술 실행 작업 생성(NEW) */
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/surgeries/tasks")
    public ResponseEntity<SurgeryExecResponse> createSurgeryTask(@Valid @RequestBody CreateSurgeryTaskRequest req) {
        return ResponseEntity.ok(service.createSurgeryTask(req));
    }

    /** 수술 스케줄 확정 = NEW -> IN_PROGRESS (IN_PROGRESS 상태에서는 재스케줄 허용) */
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/surgeries/{surgeryExecId}/schedule")
    public ResponseEntity<SurgeryExecResponse> schedule(@PathVariable Long surgeryExecId,
                                                       @Valid @RequestBody ScheduleSurgeryRequest req) {
        return ResponseEntity.ok(service.scheduleSurgery(surgeryExecId, req));
    }

    /** 수술 완료 = IN_PROGRESS -> DONE */
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/surgeries/{surgeryExecId}/complete")
    public ResponseEntity<SurgeryExecResponse> complete(@PathVariable Long surgeryExecId) {
        return ResponseEntity.ok(service.completeSurgery(surgeryExecId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @GetMapping("/surgeries")
    public ResponseEntity<List<SurgeryExecResponse>> listSurgeries(@RequestParam(required = false) Long finalOrderId) {
        return ResponseEntity.ok(service.listSurgeries(finalOrderId));
    }
}
