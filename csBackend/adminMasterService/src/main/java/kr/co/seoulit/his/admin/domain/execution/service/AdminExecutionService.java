package kr.co.seoulit.his.admin.domain.execution.service;

import kr.co.seoulit.his.admin.audit.AuditClient;
import kr.co.seoulit.his.admin.global.exception.BusinessException;
import kr.co.seoulit.his.admin.global.exception.ErrorCode;
import kr.co.seoulit.his.admin.domain.execution.*;
import kr.co.seoulit.his.admin.domain.execution.dto.*;
import kr.co.seoulit.his.admin.messaging.outbox.OutboxService;
import kr.co.seoulit.his.admin.integration.clinical.client.FinalOrderClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminExecutionService {

    private final AdmissionExecRepository admRepo;
    private final SurgeryExecRepository surgRepo;
    private final FinalOrderClient finalOrderClient;
    private final AuditClient audit;
    private final OutboxService outbox; // [ADDED][STEP8]

    @Value("${kafka.topic.admin-execution:admin.execution.v1}")
    private String topicAdminExecution;

    @Transactional(readOnly = true)
    public List<AdmissionExecResponse> listAdmissions(Long finalOrderId) {
        List<AdmissionExec> list = (finalOrderId == null)
                ? admRepo.findAll()
                : admRepo.findAllByFinalOrderId(finalOrderId);
        return list.stream().map(this::toAdm).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SurgeryExecResponse> listSurgeries(Long finalOrderId) {
        List<SurgeryExec> list = (finalOrderId == null)
                ? surgRepo.findAll()
                : surgRepo.findAllByFinalOrderId(finalOrderId);
        return list.stream().map(this::toSurg).collect(Collectors.toList());
    }

    @Transactional
    public AdmissionExecResponse admit(RecordAdmissionRequest req) {
        // compat: 기존 동작 유지(즉시 DONE 기록)
        AdmissionExec existed = admRepo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) return toAdm(existed);

        AdmissionExec saved = admRepo.save(AdmissionExec.builder()
                .finalOrderId(req.getFinalOrderId())
                .ward(req.getWard())
                .status("DONE")
                .idempotencyKey(req.getIdempotencyKey())
                .admittedAt(LocalDateTime.now())
                .dischargedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
return toAdm(saved);
    }

    // =========================
    // [ADDED][STEP6] ADT execution lifecycle
    // =========================

    /** Finalize 이후, ADT 실행 작업 생성(NEW) */
    @Transactional
    public AdmissionExecResponse createAdmissionTask(RecordAdmissionRequest req) {
        AdmissionExec existed = admRepo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) return toAdm(existed);

        Map<String, Object> fo = finalOrderClient.getFinalOrder(req.getFinalOrderId());
        String foType = (fo == null || fo.get("type") == null) ? "" : String.valueOf(fo.get("type")).toUpperCase();
        String foStatus = (fo == null || fo.get("status") == null) ? "" : String.valueOf(fo.get("status")).toUpperCase();

        if (!"ADMISSION".equals(foType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "FinalOrder.type must be ADMISSION. type=" + foType);
        }
        if (!"FINALIZED".equals(foStatus) && !"IN_PROGRESS".equals(foStatus) && !"DONE".equals(foStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "FinalOrder must be FINALIZED before ADT task. status=" + foStatus);
        }

        AdmissionExec saved = admRepo.save(AdmissionExec.builder()
                .finalOrderId(req.getFinalOrderId())
                .ward(req.getWard())
                .status("NEW")
                .idempotencyKey(req.getIdempotencyKey())
                .updatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());

        audit.write("EXECUTION_TASK_CREATED", "ADMISSION_EXEC", String.valueOf(saved.getAdmissionExecId()), null,
                Map.of("finalOrderId", saved.getFinalOrderId(), "ward", saved.getWard(), "status", saved.getStatus()));
        outbox.record("EXECUTION_TASK_CREATED", "ADMISSION_EXEC", String.valueOf(saved.getAdmissionExecId()),
                null, topicAdminExecution,
                Map.of("finalOrderId", saved.getFinalOrderId(), "status", saved.getStatus()));


        return toAdm(saved);
    }

    /** 입원(admit) = NEW -> IN_PROGRESS */
    @Transactional
    public AdmissionExecResponse startAdmission(Long admissionExecId, AdmitAdmissionRequest req) {
        AdmissionExec e = admRepo.findById(admissionExecId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "AdmissionExec not found: " + admissionExecId));

        String before = (e.getStatus() == null ? "" : e.getStatus().toUpperCase());
        if ("IN_PROGRESS".equals(before) || "DONE".equals(before)) return toAdm(e);
        if (!"NEW".equals(before)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only NEW admission task can be admitted. status=" + e.getStatus());
        }

        e.setWard(req.getWard());
        e.setStatus("IN_PROGRESS");
        e.setAdmittedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());        audit.write("EXECUTION_STARTED", "ADMISSION_EXEC", String.valueOf(e.getAdmissionExecId()), null,
                Map.of("finalOrderId", e.getFinalOrderId(), "ward", e.getWard(), "before", before, "after", e.getStatus()));
        outbox.record("EXECUTION_STARTED", "ADMISSION_EXEC", String.valueOf(e.getAdmissionExecId()),
                null, topicAdminExecution,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));


        return toAdm(e);
    }

    /** 퇴원(discharge) = IN_PROGRESS -> DONE */
    @Transactional
    public AdmissionExecResponse discharge(Long admissionExecId) {
        AdmissionExec e = admRepo.findById(admissionExecId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "AdmissionExec not found: " + admissionExecId));

        String before = (e.getStatus() == null ? "" : e.getStatus().toUpperCase());
        if ("DONE".equals(before)) return toAdm(e);
        if (!"IN_PROGRESS".equals(before)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only IN_PROGRESS admission can be discharged. status=" + e.getStatus());
        }

        e.setStatus("DONE");
        e.setDischargedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());        audit.write("EXECUTION_COMPLETED", "ADMISSION_EXEC", String.valueOf(e.getAdmissionExecId()), null,
                Map.of("finalOrderId", e.getFinalOrderId(), "ward", e.getWard(), "before", before, "after", e.getStatus()));
        outbox.record("EXECUTION_COMPLETED", "ADMISSION_EXEC", String.valueOf(e.getAdmissionExecId()),
                null, topicAdminExecution,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));


        return toAdm(e);
    }
@Transactional
    public SurgeryExecResponse recordSurgery(RecordSurgeryRequest req) {
        SurgeryExec existed = surgRepo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) return toSurg(existed);

        LocalDateTime now = LocalDateTime.now();
        SurgeryExec saved = surgRepo.save(SurgeryExec.builder()
                .finalOrderId(req.getFinalOrderId())
                .surgeryName(req.getSurgeryName())
                .status("DONE")
                .scheduledAt(now)
                .completedAt(now)
                .updatedAt(now)
                .idempotencyKey(req.getIdempotencyKey())
                .createdAt(now)
                .build());
return toSurg(saved);
    }

    // =========================
    // [ADDED][STEP7] Surgery execution lifecycle
    // =========================

    /** Finalize 이후, 수술 실행 작업 생성(NEW) */
    @Transactional
    public SurgeryExecResponse createSurgeryTask(CreateSurgeryTaskRequest req) {
        SurgeryExec existed = surgRepo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) return toSurg(existed);

        Map<String, Object> fo = finalOrderClient.getFinalOrder(req.getFinalOrderId());
        String foType = (fo == null || fo.get("type") == null) ? "" : String.valueOf(fo.get("type")).toUpperCase();
        String foStatus = (fo == null || fo.get("status") == null) ? "" : String.valueOf(fo.get("status")).toUpperCase();

        if (!"SURGERY".equals(foType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "FinalOrder.type must be SURGERY. type=" + foType);
        }
        if (!"FINALIZED".equals(foStatus) && !"IN_PROGRESS".equals(foStatus) && !"DONE".equals(foStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "FinalOrder must be FINALIZED before surgery task. status=" + foStatus);
        }

        LocalDateTime now = LocalDateTime.now();
        SurgeryExec saved = surgRepo.save(SurgeryExec.builder()
                .finalOrderId(req.getFinalOrderId())
                .surgeryName(req.getSurgeryName())
                .room(req.getRoom())
                .status("NEW")
                .idempotencyKey(req.getIdempotencyKey())
                .updatedAt(now)
                .createdAt(now)
                .build());

        audit.write("EXECUTION_TASK_CREATED", "SURGERY_EXEC", String.valueOf(saved.getSurgeryExecId()), null,
                Map.of("finalOrderId", saved.getFinalOrderId(), "surgeryName", saved.getSurgeryName(), "room", String.valueOf(saved.getRoom()), "status", saved.getStatus()));
        outbox.record("EXECUTION_TASK_CREATED", "SURGERY_EXEC", String.valueOf(saved.getSurgeryExecId()),
                null, topicAdminExecution,
                Map.of("finalOrderId", saved.getFinalOrderId(), "status", saved.getStatus()));


        return toSurg(saved);
    }

    /** 수술 스케줄 확정 = NEW -> IN_PROGRESS (IN_PROGRESS 상태에서는 재스케줄 허용) */
    @Transactional
    public SurgeryExecResponse scheduleSurgery(Long surgeryExecId, ScheduleSurgeryRequest req) {
        SurgeryExec e = surgRepo.findById(surgeryExecId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SurgeryExec not found: " + surgeryExecId));

        String before = (e.getStatus() == null ? "" : e.getStatus().toUpperCase());
        if ("DONE".equals(before)) return toSurg(e);
        if (!"NEW".equals(before) && !"IN_PROGRESS".equals(before)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only NEW/IN_PROGRESS surgery can be scheduled. status=" + e.getStatus());
        }

        e.setScheduledAt(req.getScheduledAt());
        if (req.getRoom() != null) e.setRoom(req.getRoom());
        if (req.getSurgeon() != null) e.setSurgeon(req.getSurgeon());
        e.setStatus("IN_PROGRESS");
        e.setUpdatedAt(LocalDateTime.now());        audit.write("EXECUTION_SCHEDULED", "SURGERY_EXEC", String.valueOf(e.getSurgeryExecId()), null,
                Map.of("finalOrderId", e.getFinalOrderId(), "before", before, "after", e.getStatus(), "scheduledAt", String.valueOf(e.getScheduledAt()), "room", String.valueOf(e.getRoom())));
        outbox.record("EXECUTION_SCHEDULED", "SURGERY_EXEC", String.valueOf(e.getSurgeryExecId()),
                null, topicAdminExecution,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));


        return toSurg(e);
    }

    /** 수술 완료 = IN_PROGRESS -> DONE */
    @Transactional
    public SurgeryExecResponse completeSurgery(Long surgeryExecId) {
        SurgeryExec e = surgRepo.findById(surgeryExecId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SurgeryExec not found: " + surgeryExecId));

        String before = (e.getStatus() == null ? "" : e.getStatus().toUpperCase());
        if ("DONE".equals(before)) return toSurg(e);
        if (!"IN_PROGRESS".equals(before)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only IN_PROGRESS surgery can be completed. status=" + e.getStatus());
        }

        e.setStatus("DONE");
        e.setCompletedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());        audit.write("EXECUTION_COMPLETED", "SURGERY_EXEC", String.valueOf(e.getSurgeryExecId()), null,
                Map.of("finalOrderId", e.getFinalOrderId(), "before", before, "after", e.getStatus()));
        outbox.record("EXECUTION_COMPLETED", "SURGERY_EXEC", String.valueOf(e.getSurgeryExecId()),
                null, topicAdminExecution,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));


        return toSurg(e);
    }

    private AdmissionExecResponse toAdm(AdmissionExec e) {
        return AdmissionExecResponse.builder()
                .admissionExecId(e.getAdmissionExecId())
                .finalOrderId(e.getFinalOrderId())
                .ward(e.getWard())
                .status(e.getStatus())
                .idempotencyKey(e.getIdempotencyKey())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private SurgeryExecResponse toSurg(SurgeryExec e) {
        return SurgeryExecResponse.builder()
                .surgeryExecId(e.getSurgeryExecId())
                .finalOrderId(e.getFinalOrderId())
                .surgeryName(e.getSurgeryName())
                .room(e.getRoom())
                .surgeon(e.getSurgeon())
                .status(e.getStatus())
                .idempotencyKey(e.getIdempotencyKey())
                .createdAt(e.getCreatedAt())
                .scheduledAt(e.getScheduledAt())
                .completedAt(e.getCompletedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
