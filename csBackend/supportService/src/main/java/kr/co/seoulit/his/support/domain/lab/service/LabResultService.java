package kr.co.seoulit.his.support.domain.lab.service;

import kr.co.seoulit.his.support.audit.AuditClient;
import kr.co.seoulit.his.support.global.common.CurrentUserUtil;
import kr.co.seoulit.his.support.global.exception.BusinessException;
import kr.co.seoulit.his.support.global.exception.ErrorCode;
import kr.co.seoulit.his.support.domain.lab.LabResult;
import kr.co.seoulit.his.support.domain.lab.LabResultRepository;
import kr.co.seoulit.his.support.domain.lab.dto.CreateLabResultRequest;
import kr.co.seoulit.his.support.domain.lab.dto.LabResultResponse;
import kr.co.seoulit.his.support.domain.lab.dto.UpdateLabResultRequest;
import kr.co.seoulit.his.support.domain.lab.mapper.LabResultMapper;
import kr.co.seoulit.his.support.messaging.outbox.QueueEventOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LabResultService {

    private final LabResultRepository repo;
    private final LabResultMapper mapper;
    private final AuditClient audit;
    private final QueueEventOutboxService outbox;

    @Transactional
    public LabResultResponse create(CreateLabResultRequest req) {
        // ✅ 멱등: 동일 idempotencyKey 결과가 있으면 기존 결과 반환
        return repo.findByIdempotencyKey(req.idempotencyKey())
                .map(mapper::toResponse)
                .orElseGet(() -> {
                    if (repo.existsByOrderIdAndArchivedFalse(req.orderId())) {
                        throw new BusinessException(ErrorCode.INVALID_STATE, "이미 결과가 등록된 ORDER 입니다. orderId=" + req.orderId());
                    }

                    LabResult r = LabResult.builder()
                            .orderId(req.orderId())
                            .resultText(req.resultText())
                            .status("RECORDED")
                            .idempotencyKey(req.idempotencyKey())
                            .createdAt(LocalDateTime.now())
                            .archived(false)
                            .build();

                    LabResult saved;
                    try {
                        saved = repo.save(r);
                    } catch (DataIntegrityViolationException e) {
                        // 경쟁 상황에서 UNIQUE(idempotencyKey)로 충돌하면 기존 결과를 반환(멱등 보장)
                        return repo.findByIdempotencyKey(req.idempotencyKey())
                                .map(mapper::toResponse)
                                .orElseThrow(() -> e);
                    }

                    // ✅ 대기열 자동 갱신 이벤트 (Outbox+Idempotency)
                    outbox.enqueue("QUEUE_COMPLETED", saved.getOrderId(), "LAB",
                            Map.of("resultType", "LAB", "resultId", saved.getLabResultId()));

                    // ✅ 감사로그
                    audit.write("RESULT_RECORDED", "LAB_RESULT", String.valueOf(saved.getLabResultId()), null,
                            Map.of("orderId", saved.getOrderId(), "status", saved.getStatus()));

                    return mapper.toResponse(saved);
                });
    }

    @Transactional(readOnly = true)
    public LabResultResponse get(Long id, boolean includeArchived) {
        LabResult r = includeArchived
                ? repo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "LabResult not found. id=" + id))
                : repo.findByLabResultIdAndArchivedFalse(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "LabResult not found. id=" + id));
        return mapper.toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<LabResultResponse> list(Long orderId) {
        if (orderId == null) return List.of();
        return repo.findByOrderIdAndArchivedFalseOrderByCreatedAtDesc(orderId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public LabResultResponse update(Long id, UpdateLabResultRequest req) {
        LabResult r = repo.findByLabResultIdAndArchivedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "LabResult not found. id=" + id));
        r.setResultText(req.resultText());
        r.setUpdatedAt(LocalDateTime.now());
        r.setUpdatedBy(CurrentUserUtil.loginIdOrSystem());

        LabResult saved = repo.save(r);
        audit.write("RESULT_UPDATED", "LAB_RESULT", String.valueOf(saved.getLabResultId()), null,
                Map.of("orderId", saved.getOrderId(), "reason", req.reason()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public LabResultResponse archive(Long id, String reason) {
        LabResult r = repo.findByLabResultIdAndArchivedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "LabResult not found. id=" + id));
        r.setArchived(true);
        r.setArchivedAt(LocalDateTime.now());
        r.setArchivedBy(CurrentUserUtil.loginIdOrSystem());
        r.setArchivedReason(reason);

        LabResult saved = repo.save(r);
        audit.write("RESULT_ARCHIVED", "LAB_RESULT", String.valueOf(saved.getLabResultId()), null,
                Map.of("orderId", saved.getOrderId(), "reason", reason));
        return mapper.toResponse(saved);
    }
}
