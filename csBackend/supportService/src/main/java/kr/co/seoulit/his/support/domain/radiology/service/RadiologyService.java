package kr.co.seoulit.his.support.domain.radiology.service;

import kr.co.seoulit.his.support.audit.AuditClient;
import kr.co.seoulit.his.support.global.common.CurrentUserUtil;
import kr.co.seoulit.his.support.global.exception.BusinessException;
import kr.co.seoulit.his.support.global.exception.ErrorCode;
import kr.co.seoulit.his.support.messaging.outbox.QueueEventOutboxService;
import kr.co.seoulit.his.support.domain.radiology.RadiologyReport;
import kr.co.seoulit.his.support.domain.radiology.RadiologyReportRepository;
import kr.co.seoulit.his.support.domain.radiology.dto.CreateRadiologyReportRequest;
import kr.co.seoulit.his.support.domain.radiology.dto.RadiologyReportResponse;
import kr.co.seoulit.his.support.domain.radiology.dto.UpdateRadiologyReportRequest;
import kr.co.seoulit.his.support.domain.radiology.mapper.RadiologyReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RadiologyService {

    private final RadiologyReportRepository repo;
    private final RadiologyReportMapper mapper;
    private final AuditClient audit;
    private final QueueEventOutboxService outbox;

    @Transactional
    public RadiologyReportResponse create(CreateRadiologyReportRequest req) {
        return repo.findByIdempotencyKey(req.idempotencyKey())
                .map(mapper::toResponse)
                .orElseGet(() -> {
                    if (repo.existsByOrderIdAndArchivedFalse(req.orderId())) {
                        throw new BusinessException(ErrorCode.INVALID_STATE, "이미 리포트가 등록된 ORDER 입니다. orderId=" + req.orderId());
                    }

                    RadiologyReport r = RadiologyReport.builder()
                            .orderId(req.orderId())
                            .reportText(req.reportText())
                            .status("RECORDED")
                            .idempotencyKey(req.idempotencyKey())
                            .createdAt(LocalDateTime.now())
                            .archived(false)
                            .build();

                    RadiologyReport saved;
                    try {
                        saved = repo.save(r);
                    } catch (DataIntegrityViolationException e) {
                        // 경쟁 상황에서 UNIQUE(idempotencyKey)로 충돌하면 기존 결과를 반환(멱등 보장)
                        return repo.findByIdempotencyKey(req.idempotencyKey())
                                .map(mapper::toResponse)
                                .orElseThrow(() -> e);
                    }

                    // ✅ 대기열 자동 갱신 이벤트 (Outbox+Idempotency)
                    outbox.enqueue("QUEUE_COMPLETED", saved.getOrderId(), "RAD",
                            Map.of("resultType", "RAD", "resultId", saved.getRadiologyReportId()));

                    // ✅ 감사로그
                    audit.write("RESULT_RECORDED", "RAD_REPORT", String.valueOf(saved.getRadiologyReportId()), null,
                            Map.of("orderId", saved.getOrderId(), "status", saved.getStatus()));

                    return mapper.toResponse(saved);
                });
    }

    @Transactional(readOnly = true)
    public RadiologyReportResponse get(Long id, boolean includeArchived) {
        RadiologyReport r = includeArchived
                ? repo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "RadiologyReport not found. id=" + id))
                : repo.findByRadiologyReportIdAndArchivedFalse(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "RadiologyReport not found. id=" + id));
        return mapper.toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<RadiologyReportResponse> list(Long orderId) {
        if (orderId == null) return List.of();
        return repo.findByOrderIdAndArchivedFalseOrderByCreatedAtDesc(orderId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public RadiologyReportResponse update(Long id, UpdateRadiologyReportRequest req) {
        RadiologyReport r = repo.findByRadiologyReportIdAndArchivedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "RadiologyReport not found. id=" + id));
        r.setReportText(req.reportText());
        r.setUpdatedAt(LocalDateTime.now());
        r.setUpdatedBy(CurrentUserUtil.loginIdOrSystem());

        RadiologyReport saved = repo.save(r);
        audit.write("RESULT_UPDATED", "RAD_REPORT", String.valueOf(saved.getRadiologyReportId()), null,
                Map.of("orderId", saved.getOrderId(), "reason", req.reason()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public RadiologyReportResponse archive(Long id, String reason) {
        RadiologyReport r = repo.findByRadiologyReportIdAndArchivedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "RadiologyReport not found. id=" + id));
        r.setArchived(true);
        r.setArchivedAt(LocalDateTime.now());
        r.setArchivedBy(CurrentUserUtil.loginIdOrSystem());
        r.setArchivedReason(reason);

        RadiologyReport saved = repo.save(r);
        audit.write("RESULT_ARCHIVED", "RAD_REPORT", String.valueOf(saved.getRadiologyReportId()), null,
                Map.of("orderId", saved.getOrderId(), "reason", reason));
        return mapper.toResponse(saved);
    }
}
