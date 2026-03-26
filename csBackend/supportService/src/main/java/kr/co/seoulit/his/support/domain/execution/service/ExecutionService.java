package kr.co.seoulit.his.support.domain.execution.service;

import kr.co.seoulit.his.support.audit.AuditClient;
import kr.co.seoulit.his.support.integration.clinical.client.FinalOrderClient;
import kr.co.seoulit.his.support.global.exception.BusinessException;
import kr.co.seoulit.his.support.global.exception.ErrorCode;
import kr.co.seoulit.his.support.domain.execution.*;
import kr.co.seoulit.his.support.domain.execution.dto.*;
import kr.co.seoulit.his.support.messaging.outbox.OutboxService;
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
public class ExecutionService {

    private final InjectionExecRepository injRepo;
    private final EndoscopyReportRepository endoRepo;
    private final MedExecRepository medRepo;

    private final FinalOrderClient finalOrderClient;
    private final AuditClient audit;

    private final OutboxService outbox; // [ADDED][STEP8]

    @Value("${kafka.topic.support-execution:support.execution.v1}")
    private String topicSupportExecution;

    // --------------------
    // Injection (INJECTION)
    // --------------------

    @Transactional(readOnly = true)
    public List<InjectionExecResponse> listInjections(Long finalOrderId) {
        List<InjectionExec> list = (finalOrderId == null)
                ? injRepo.findAll()
                : injRepo.findAllByFinalOrderId(finalOrderId);
        return list.stream().map(this::toInj).collect(Collectors.toList());
    }

    /** [STEP5] 실행 생성(NEW) */
    @Transactional
    public InjectionExecResponse createInjectionTask(CreateInjectionTaskRequest req) {
        requireFinalized(req.getFinalOrderId(), "INJECTION task create");

        InjectionExec existed = injRepo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) return toInj(existed);

        InjectionExec saved = injRepo.save(InjectionExec.builder()
                .finalOrderId(req.getFinalOrderId())
                .status("NEW")
                .note(req.getNote())
                .idempotencyKey(req.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build());

        audit.write("EXECUTION_TASK_CREATED", "INJECTION_EXEC", String.valueOf(saved.getInjectionExecId()), null,
                Map.of("finalOrderId", saved.getFinalOrderId(), "status", saved.getStatus()));
        outbox.record("EXECUTION_TASK_CREATED", "INJECTION_EXEC", String.valueOf(saved.getInjectionExecId()),
                String.valueOf(saved.getFinalOrderId()), topicSupportExecution,
                Map.of("finalOrderId", saved.getFinalOrderId(), "status", saved.getStatus()));


        return toInj(saved);
    }

    /** [STEP5] 착수(IN_PROGRESS) */
    @Transactional
    public InjectionExecResponse startInjection(Long injectionExecId) {
        InjectionExec e = injRepo.findById(injectionExecId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "InjectionExec not found: " + injectionExecId));

        if ("DONE".equalsIgnoreCase(e.getStatus())) return toInj(e);
        if ("IN_PROGRESS".equalsIgnoreCase(e.getStatus())) return toInj(e);

        // NEW -> IN_PROGRESS
        e.setStatus("IN_PROGRESS");        audit.write("EXECUTION_STARTED", "INJECTION_EXEC", String.valueOf(e.getInjectionExecId()), null,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));
        outbox.record("EXECUTION_STARTED", "INJECTION_EXEC", String.valueOf(e.getInjectionExecId()),
                String.valueOf(e.getFinalOrderId()), topicSupportExecution,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));


        return toInj(e);
    }

    /** [STEP5] 완료(DONE) */
    @Transactional
    public InjectionExecResponse completeInjection(Long injectionExecId, CompleteInjectionRequest req) {
        InjectionExec e = injRepo.findById(injectionExecId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "InjectionExec not found: " + injectionExecId));

        if ("DONE".equalsIgnoreCase(e.getStatus())) return toInj(e);

        e.setStatus("DONE");
        e.setNote(req.getNote());        audit.write("EXECUTION_COMPLETED", "INJECTION_EXEC", String.valueOf(e.getInjectionExecId()), null,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));
        outbox.record("EXECUTION_COMPLETED", "INJECTION_EXEC", String.valueOf(e.getInjectionExecId()),
                String.valueOf(e.getFinalOrderId()), topicSupportExecution,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));


        return toInj(e);
    }

    /**
     * [Compat] 기존 POST /injections 는 "완료 기록" 형태였음.
     * 유지 목적: (없으면 생성 -> IN_PROGRESS -> DONE)으로 한 번에 처리.
     */
    @Transactional
    public InjectionExecResponse recordInjection(RecordInjectionRequest req) {
        // FINALIZED 이후에만 완료 기록 가능
        requireFinalizedOrInProgress(req.getFinalOrderId(), "INJECTION record");

        InjectionExec existed = injRepo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) {
            // 이미 존재하면 DONE까지 처리되어 있을 가능성이 높으므로 그대로 반환
            return toInj(existed);
        }

        InjectionExec saved = injRepo.save(InjectionExec.builder()
                .finalOrderId(req.getFinalOrderId())
                .status("IN_PROGRESS")
                .note(req.getNote())
                .idempotencyKey(req.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build());        // 즉시 완료 처리
        saved.setStatus("DONE");        audit.write("EXECUTION_RECORDED", "INJECTION_EXEC", String.valueOf(saved.getInjectionExecId()), null,
                Map.of("finalOrderId", saved.getFinalOrderId(), "status", saved.getStatus()));

        return toInj(saved);
    }

    // --------------
    // Medication (MED)
    // --------------

    @Transactional(readOnly = true)
    public List<MedExecResponse> listMedExecs(Long finalOrderId) {
        List<MedExec> list = (finalOrderId == null)
                ? medRepo.findAll()
                : medRepo.findAllByFinalOrderId(finalOrderId);
        return list.stream().map(this::toMed).collect(Collectors.toList());
    }

    /** [STEP5] 실행 생성(NEW) */
    @Transactional
    public MedExecResponse createMedExecTask(CreateMedExecTaskRequest req) {
        requireFinalized(req.getFinalOrderId(), "MED task create");

        MedExec existed = medRepo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) return toMed(existed);

        MedExec saved = medRepo.save(MedExec.builder()
                .finalOrderId(req.getFinalOrderId())
                .status("NEW")
                .note(req.getNote())
                .idempotencyKey(req.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build());

        audit.write("EXECUTION_TASK_CREATED", "MED_EXEC", String.valueOf(saved.getMedExecId()), null,
                Map.of("finalOrderId", saved.getFinalOrderId(), "status", saved.getStatus()));
        outbox.record("EXECUTION_TASK_CREATED", "MED_EXEC", String.valueOf(saved.getMedExecId()),
                String.valueOf(saved.getFinalOrderId()), topicSupportExecution,
                Map.of("finalOrderId", saved.getFinalOrderId(), "status", saved.getStatus()));


        return toMed(saved);
    }

    /** [STEP5] 착수(IN_PROGRESS) */
    @Transactional
    public MedExecResponse startMedExec(Long medExecId) {
        MedExec e = medRepo.findById(medExecId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "MedExec not found: " + medExecId));

        if ("DONE".equalsIgnoreCase(e.getStatus())) return toMed(e);
        if ("IN_PROGRESS".equalsIgnoreCase(e.getStatus())) return toMed(e);

        e.setStatus("IN_PROGRESS");        audit.write("EXECUTION_STARTED", "MED_EXEC", String.valueOf(e.getMedExecId()), null,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));
        outbox.record("EXECUTION_STARTED", "MED_EXEC", String.valueOf(e.getMedExecId()),
                String.valueOf(e.getFinalOrderId()), topicSupportExecution,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));


        return toMed(e);
    }

    /** [STEP5] 완료(DONE) */
    @Transactional
    public MedExecResponse completeMedExec(Long medExecId, CompleteMedExecRequest req) {
        MedExec e = medRepo.findById(medExecId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "MedExec not found: " + medExecId));

        if ("DONE".equalsIgnoreCase(e.getStatus())) return toMed(e);

        e.setStatus("DONE");
        e.setNote(req.getNote());        audit.write("EXECUTION_COMPLETED", "MED_EXEC", String.valueOf(e.getMedExecId()), null,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));
        outbox.record("EXECUTION_COMPLETED", "MED_EXEC", String.valueOf(e.getMedExecId()),
                String.valueOf(e.getFinalOrderId()), topicSupportExecution,
                Map.of("finalOrderId", e.getFinalOrderId(), "status", e.getStatus()));


        return toMed(e);
    }

    // --------------------
    // Endoscopy (PROC) - 기존 방식(완료 기록)
    // --------------------

    @Transactional(readOnly = true)
    public List<EndoscopyReportResponse> listEndoscopyReports(Long finalOrderId) {
        List<EndoscopyReport> list = (finalOrderId == null)
                ? endoRepo.findAll()
                : endoRepo.findAllByFinalOrderId(finalOrderId);
        return list.stream().map(this::toEndo).collect(Collectors.toList());
    }

    @Transactional
    public EndoscopyReportResponse recordEndoscopy(RecordEndoscopyReportRequest req) {
        // FINALIZED 이후에만 결과 기록 가능
        requireFinalizedOrInProgress(req.getFinalOrderId(), "ENDOSCOPY record");

        EndoscopyReport existed = endoRepo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) return toEndo(existed);

        EndoscopyReport saved = endoRepo.save(EndoscopyReport.builder()
                .finalOrderId(req.getFinalOrderId())
                .status("DONE")
                .reportText(req.getReportText())
                .idempotencyKey(req.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build());        audit.write("EXECUTION_RECORDED", "ENDOSCOPY_REPORT", String.valueOf(saved.getEndoscopyReportId()), null,
                Map.of("finalOrderId", saved.getFinalOrderId(), "status", saved.getStatus()));

        return toEndo(saved);
    }

    // --------------------
    // Guards
    // --------------------

    private void requireFinalized(Long finalOrderId, String action) {
        String st = finalOrderClient.getStatus(finalOrderId);
        if (st == null) throw new BusinessException(ErrorCode.NOT_FOUND, "FinalOrder not found: " + finalOrderId);
        if (!"FINALIZED".equalsIgnoreCase(st)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, action + " requires FINALIZED. status=" + st);
        }
    }

    private void requireFinalizedOrInProgress(Long finalOrderId, String action) {
        String st = finalOrderClient.getStatus(finalOrderId);
        if (st == null) throw new BusinessException(ErrorCode.NOT_FOUND, "FinalOrder not found: " + finalOrderId);
        if (!("FINALIZED".equalsIgnoreCase(st) || "IN_PROGRESS".equalsIgnoreCase(st) || "DONE".equalsIgnoreCase(st))) {
            throw new BusinessException(ErrorCode.INVALID_STATE, action + " requires FINALIZED/IN_PROGRESS. status=" + st);
        }
    }

    // --------------------
    // Mappers
    // --------------------

    private InjectionExecResponse toInj(InjectionExec e) {
        return InjectionExecResponse.builder()
                .injectionExecId(e.getInjectionExecId())
                .finalOrderId(e.getFinalOrderId())
                .status(e.getStatus())
                .note(e.getNote())
                .idempotencyKey(e.getIdempotencyKey())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private EndoscopyReportResponse toEndo(EndoscopyReport e) {
        return EndoscopyReportResponse.builder()
                .endoscopyReportId(e.getEndoscopyReportId())
                .finalOrderId(e.getFinalOrderId())
                .status(e.getStatus())
                .reportText(e.getReportText())
                .idempotencyKey(e.getIdempotencyKey())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private MedExecResponse toMed(MedExec e) {
        return MedExecResponse.builder()
                .medExecId(e.getMedExecId())
                .finalOrderId(e.getFinalOrderId())
                .status(e.getStatus())
                .note(e.getNote())
                .idempotencyKey(e.getIdempotencyKey())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
