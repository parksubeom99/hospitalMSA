package kr.co.seoulit.his.clinical.domain.emr.soap.service;

import kr.co.seoulit.his.clinical.audit.AuditClient;
import kr.co.seoulit.his.clinical.domain.order.OrderRepository;
import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import kr.co.seoulit.his.clinical.messaging.outbox.OutboxService;
import kr.co.seoulit.his.clinical.global.response.CurrentUserUtil;
import kr.co.seoulit.his.clinical.domain.emr.soap.SoapNote;
import kr.co.seoulit.his.clinical.domain.emr.soap.SoapNoteRepository;
import kr.co.seoulit.his.clinical.domain.emr.soap.dto.SoapArchiveRequest;
import kr.co.seoulit.his.clinical.domain.emr.soap.dto.SoapResponse;
import kr.co.seoulit.his.clinical.domain.emr.soap.dto.SoapUpsertRequest;
import kr.co.seoulit.his.clinical.domain.emr.soap.dto.SoapVersionResponse;
import kr.co.seoulit.his.clinical.domain.emr.soap.history.SoapNoteHistory;
import kr.co.seoulit.his.clinical.domain.emr.soap.history.SoapNoteHistoryRepository;
import kr.co.seoulit.his.clinical.domain.emr.soap.mapper.SoapNoteMapper;
import kr.co.seoulit.his.clinical.global.exception.BusinessException;
import kr.co.seoulit.his.clinical.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SoapNoteService {

    private final SoapNoteRepository repo;
    private final SoapNoteHistoryRepository historyRepo;
    private final SoapNoteMapper mapper;
    private final AuditClient audit;

    // Phase 2: 상태머신 + 이벤트 발행
    private final VisitClinicalStatusService visitStatusSvc;
    private final OrderRepository orderRepo;
    private final OutboxService outbox;

    @Value("${kafka.topic.his-diagnostic-order-submitted:his.clinical.diagnostic-order.submitted}")
    private String topicDiagnosticSubmitted;

    @Transactional
    public SoapResponse upsert(Long visitId, SoapUpsertRequest req) {
        SoapNote note = repo.findById(visitId).orElseGet(() ->
                SoapNote.builder()
                        .visitId(visitId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        if (note.getCreatedAt() == null) {
            note.setCreatedAt(note.getUpdatedAt() != null ? note.getUpdatedAt() : LocalDateTime.now());
        }
        if (note.getUpdatedAt() == null) {
            note.setUpdatedAt(LocalDateTime.now());
        }

        if (note.isArchived()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Archived SOAP cannot be modified. visitId=" + visitId);
        }

        boolean existed = repo.existsById(visitId);

        if (existed && note.getVersionNo() != null && note.getVersionNo() >= 1) {
            historyRepo.save(SoapNoteHistory.builder()
                    .visitId(visitId)
                    .versionNo(note.getVersionNo())
                    .subjective(note.getSubjective())
                    .objective(note.getObjective())
                    .assessment(note.getAssessment())
                    .plan(note.getPlan())
                    .capturedAt(LocalDateTime.now())
                    .capturedBy(CurrentUserUtil.currentLoginIdOrNull())
                    .build());
            note.setVersionNo(note.getVersionNo() + 1);
        } else {
            note.setVersionNo(1);
        }

        note.setSubjective(req.subjective());
        note.setObjective(req.objective());
        note.setAssessment(req.assessment());
        note.setPlan(req.plan());
        note.setUpdatedAt(LocalDateTime.now());

        SoapNote saved = repo.save(note);

        audit.write("EMR_WRITTEN", "SOAP_NOTE", String.valueOf(saved.getVisitId()), null,
                Map.of("visitId", saved.getVisitId(), "versionNo", saved.getVersionNo()));

        // Phase 2: SOAP 저장 시 검사요청 존재 여부 확인 → 상태 전환 + 이벤트 발행
        tryAdvanceToDiagnosticSubmitted(visitId);

        return mapper.toResponse(saved);
    }

    /**
     * Phase 2: SOAP 저장 후 검사오더가 존재하면 EXAM_REQUESTED 전환 및
     * DIAGNOSTIC_ORDER_SUBMITTED 이벤트 발행 시도
     *
     * - SOAP과 검사요청이 모두 완료된 시점에 단 한 번 발행 (idempotent)
     * - orderRepo.findByVisitIdAndDeletedFalse로 진단용 오더 존재 여부 확인
     */
    private void tryAdvanceToDiagnosticSubmitted(Long visitId) {
        try {
            boolean hasExamOrder = !orderRepo.findByVisitIdAndDeletedFalse(visitId).isEmpty();
            boolean transitioned = false;
            if (hasExamOrder) {
                transitioned = visitStatusSvc.markExamRequested(visitId);
            } else {
                visitStatusSvc.markSoapDone(visitId, false);
            }

            if (transitioned) {
                visitStatusSvc.markExamInProgress(visitId);
                outbox.record(
                        "DIAGNOSTIC_ORDER_SUBMITTED",
                        "VISIT",
                        String.valueOf(visitId),
                        String.valueOf(visitId),
                        topicDiagnosticSubmitted,
                        Map.of("visitId", visitId, "triggeredBy", "SOAP_SAVED")
                );
                log.info("[Phase2] DIAGNOSTIC_ORDER_SUBMITTED published. visitId={}", visitId);
            }
        } catch (Exception e) {
            // 이벤트 발행 실패가 SOAP 저장 트랜잭션을 롤백하지 않도록 catch
            log.warn("[Phase2] Failed to publish DIAGNOSTIC_ORDER_SUBMITTED. visitId={}", visitId, e);
        }
    }

    /**
     * [FIXED] SOAP 조회 - 없으면 빈 SOAP 반환 (404 → 200 빈값)
     */
    @Transactional(readOnly = true)
    public SoapResponse get(Long visitId) {
        return repo.findById(visitId)
                .map(mapper::toResponse)
                .orElseGet(() -> emptyResponse(visitId));
    }

    private SoapResponse emptyResponse(Long visitId) {
        return new SoapResponse(
                visitId,
                "",   // subjective
                "",   // objective
                "",   // assessment
                "",   // plan
                0,    // versionNo
                false,// archived
                null, // createdAt
                null, // updatedAt
                null, // archivedAt
                null, // archivedBy
                null  // archivedReason
        );
    }

    @Transactional(readOnly = true)
    public List<SoapVersionResponse> history(Long visitId) {
        SoapNote current = repo.findById(visitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SOAP Note not found. visitId=" + visitId));

        List<SoapVersionResponse> out = new ArrayList<>();

        out.add(new SoapVersionResponse(
                visitId,
                current.getVersionNo(),
                current.getUpdatedAt(),
                null,
                true
        ));

        for (SoapNoteHistory h : historyRepo.findByVisitIdOrderByVersionNoDesc(visitId)) {
            out.add(new SoapVersionResponse(
                    visitId,
                    h.getVersionNo(),
                    h.getCapturedAt(),
                    h.getCapturedBy(),
                    false
            ));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public SoapResponse getVersion(Long visitId, Integer versionNo) {
        SoapNote current = repo.findById(visitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SOAP Note not found. visitId=" + visitId));

        if (current.getVersionNo() != null && current.getVersionNo().equals(versionNo)) {
            return mapper.toResponse(current);
        }

        SoapNoteHistory h = historyRepo.findByVisitIdAndVersionNo(visitId, versionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "SOAP version not found. visitId=" + visitId + ", versionNo=" + versionNo));

        return new SoapResponse(
                visitId,
                h.getSubjective(),
                h.getObjective(),
                h.getAssessment(),
                h.getPlan(),
                versionNo,
                current.isArchived(),
                current.getCreatedAt(),
                h.getCapturedAt(),
                current.getArchivedAt(),
                current.getArchivedBy(),
                current.getArchivedReason()
        );
    }

    @Transactional
    public SoapResponse archive(Long visitId, SoapArchiveRequest req) {
        SoapNote note = repo.findById(visitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SOAP Note not found. visitId=" + visitId));

        if (note.isArchived()) {
            return mapper.toResponse(note);
        }

        note.setArchived(true);
        note.setArchivedAt(LocalDateTime.now());
        note.setArchivedBy(CurrentUserUtil.currentLoginIdOrNull());
        note.setArchivedReason(req == null ? null : req.reason());
        note.setUpdatedAt(LocalDateTime.now());

        SoapNote saved = repo.save(note);

        audit.write("SOAP_ARCHIVED", "SOAP_NOTE", String.valueOf(saved.getVisitId()), null,
                Map.of("visitId", saved.getVisitId(), "archived", true));

        return mapper.toResponse(saved);
    }
}
