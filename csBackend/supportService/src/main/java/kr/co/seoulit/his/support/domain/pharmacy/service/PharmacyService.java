package kr.co.seoulit.his.support.domain.pharmacy.service;

import kr.co.seoulit.his.support.audit.AuditClient;
import kr.co.seoulit.his.support.integration.clinical.client.OrderClient;
import kr.co.seoulit.his.support.global.exception.BusinessException;
import kr.co.seoulit.his.support.global.exception.ErrorCode;
import kr.co.seoulit.his.support.global.common.CurrentUserUtil;
import kr.co.seoulit.his.support.domain.pharmacy.Dispense;
import kr.co.seoulit.his.support.domain.pharmacy.DispenseRepository;
import kr.co.seoulit.his.support.domain.pharmacy.dto.CreateDispenseRequest;
import kr.co.seoulit.his.support.domain.pharmacy.dto.DispenseResponse;
import kr.co.seoulit.his.support.domain.pharmacy.dto.UpdateDispenseRequest;
import kr.co.seoulit.his.support.domain.pharmacy.mapper.DispenseMapper;
import kr.co.seoulit.his.support.messaging.outbox.QueueEventOutboxService;
import kr.co.seoulit.his.support.domain.worklist.service.WorklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PharmacyService {

    private final DispenseRepository repo;
    private final DispenseMapper mapper;
    private final OrderClient orders;
    private final AuditClient audit;
    private final QueueEventOutboxService outbox;
    private final WorklistService worklists;

    @Transactional
    public DispenseResponse dispense(CreateDispenseRequest req) {
        return repo.findByIdempotencyKey(req.idempotencyKey())
                .map(mapper::toResponse)
                .orElseGet(() -> {
                    if (repo.existsByOrderIdAndArchivedFalse(req.orderId())) {
                        throw new BusinessException(ErrorCode.INVALID_STATE, "이미 조제된 ORDER 입니다. orderId=" + req.orderId());
                    }

                    Dispense d = Dispense.builder()
                            .orderId(req.orderId())
                            .dispenseText(req.note())
                            .status("DISPENSED")
                            .idempotencyKey(req.idempotencyKey())
                            .createdAt(LocalDateTime.now())
                            .build();

                    Dispense saved;
                    try {
                        saved = repo.save(d);
                    } catch (DataIntegrityViolationException e) {
                        // 경쟁 상황에서 UNIQUE(idempotencyKey)로 충돌하면 기존 결과를 반환(멱등 보장)
                        return repo.findByIdempotencyKey(req.idempotencyKey())
                                .map(mapper::toResponse)
                                .orElseThrow(() -> e);
                    }

                    orders.markDone(saved.getOrderId());

                    // ✅ Worklist 상태도 DONE으로 동기화
                    worklists.completeWork(saved.getOrderId(), "PHARM");

                    // ✅ 대기열 자동 갱신 이벤트 (Outbox+Idempotency)
                    outbox.enqueue("QUEUE_COMPLETED", saved.getOrderId(), "PHARM",
                            Map.of("resultType", "PHARM", "resultId", saved.getDispenseId()));

                    audit.write("RESULT_RECORDED", "PHARM_DISPENSE", String.valueOf(saved.getDispenseId()), null,
                            Map.of("orderId", saved.getOrderId(), "status", saved.getStatus()));

                    return mapper.toResponse(saved);
                });
    }

    @Transactional(readOnly = true)
    public DispenseResponse get(Long id, boolean includeArchived) {
        Dispense d = includeArchived
                ? repo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Dispense not found. id=" + id))
                : repo.findByDispenseIdAndArchivedFalse(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Dispense not found. id=" + id));
        return mapper.toResponse(d);
    }

    @Transactional
    public DispenseResponse update(Long id, UpdateDispenseRequest req) {
        Dispense d = repo.findByDispenseIdAndArchivedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Dispense not found. id=" + id));
        d.setDispenseText(req.note());
        d.setUpdatedAt(LocalDateTime.now());
        d.setUpdatedBy(CurrentUserUtil.loginIdOrSystem());

        Dispense saved = repo.save(d);
        audit.write("RESULT_UPDATED", "PHARM_DISPENSE", String.valueOf(saved.getDispenseId()), null,
                Map.of("orderId", saved.getOrderId(), "reason", req.reason()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public DispenseResponse archive(Long id, String reason) {
        Dispense d = repo.findByDispenseIdAndArchivedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Dispense not found. id=" + id));
        d.setArchived(true);
        d.setArchivedAt(LocalDateTime.now());
        d.setArchivedBy(CurrentUserUtil.loginIdOrSystem());
        d.setArchivedReason(reason);

        Dispense saved = repo.save(d);
        audit.write("RESULT_ARCHIVED", "PHARM_DISPENSE", String.valueOf(saved.getDispenseId()), null,
                Map.of("orderId", saved.getOrderId(), "reason", reason));
        return mapper.toResponse(saved);
    }
}
