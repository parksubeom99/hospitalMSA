package kr.co.seoulit.his.clinical.domain.finalorder.service;

import kr.co.seoulit.his.clinical.audit.AuditClient;
import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import kr.co.seoulit.his.clinical.messaging.outbox.OutboxService;
import kr.co.seoulit.his.clinical.global.exception.BusinessException;
import kr.co.seoulit.his.clinical.global.exception.ErrorCode;
import kr.co.seoulit.his.clinical.domain.finalorder.FinalOrder;
import kr.co.seoulit.his.clinical.domain.finalorder.FinalOrderRepository;
import kr.co.seoulit.his.clinical.domain.finalorder.dto.CreateFinalOrderRequest;
import kr.co.seoulit.his.clinical.domain.finalorder.dto.FinalOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinalOrderService {

    private final FinalOrderRepository repo;
    private final AuditClient audit;
    private final OutboxService outbox;

    // Phase 2: Visit clinical 상태머신
    private final VisitClinicalStatusService visitStatusSvc;

    @Value("${kafka.topic.clinical-finalorder:clinical.finalorder.v1}")
    private String topicFinalOrder;

    @Transactional
    public FinalOrderResponse create(CreateFinalOrderRequest req) {
        FinalOrder existed = repo.findByIdempotencyKey(req.getIdempotencyKey()).orElse(null);
        if (existed != null) return toResponse(existed);

        LocalDateTime now = LocalDateTime.now();
        FinalOrder saved = repo.save(FinalOrder.builder()
                .visitId(req.getVisitId())
                .type(req.getType())
                .status("ORDERED")
                .note(req.getNote())
                .idempotencyKey(req.getIdempotencyKey())
                .createdAt(now)
                .updatedAt(now)
                .build());

        audit.write("FINAL_ORDER_CREATED", "FINAL_ORDER", String.valueOf(saved.getFinalOrderId()), null,
                Map.of("visitId", saved.getVisitId(), "type", saved.getType(), "status", saved.getStatus()));
        outbox.record("FINAL_ORDER_CREATED", "FINAL_ORDER", String.valueOf(saved.getFinalOrderId()),
                String.valueOf(saved.getFinalOrderId()), topicFinalOrder,
                Map.of("finalOrderId", saved.getFinalOrderId(), "visitId", saved.getVisitId(),
                        "type", saved.getType(), "status", saved.getStatus()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FinalOrderResponse> list(String status, String type) {
        return repo.findAll().stream()
                .filter(o -> status == null || status.isBlank() || status.equalsIgnoreCase(o.getStatus()))
                .filter(o -> type == null || type.isBlank() || type.equalsIgnoreCase(o.getType()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FinalOrderResponse updateStatus(Long id, String status) {
        FinalOrder o = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("FinalOrder not found: " + id));
        String before = o.getStatus();
        o.setStatus(status);
        o.setUpdatedAt(LocalDateTime.now());

        audit.write("FINAL_ORDER_STATUS_CHANGED", "FINAL_ORDER", String.valueOf(o.getFinalOrderId()), null,
                Map.of("before", before, "after", o.getStatus(), "visitId", o.getVisitId(), "type", o.getType()));
        outbox.record("FINAL_ORDER_STATUS_CHANGED", "FINAL_ORDER", String.valueOf(o.getFinalOrderId()),
                String.valueOf(o.getFinalOrderId()), topicFinalOrder,
                Map.of("finalOrderId", o.getFinalOrderId(), "visitId", o.getVisitId(),
                        "type", o.getType(), "before", before, "after", o.getStatus()));
        return toResponse(o);
    }

    /**
     * Phase 2: 최종오더 확정 (Finalize)
     *
     * 정책:
     * - ORDERED 상태에서만 FINALIZED 전이 가능
     * - 이미 FINALIZED면 멱등 반환
     * - FINAL_ORDER_FINALIZED 이벤트 Outbox 기록 (Support/Admin consumer가 수신)
     * - Phase 2: visitId 기준 VisitClinicalStatus → FINAL_ORDER_CONFIRMED 전환
     *
     * 주의: FINAL_ORDER_READY 상태 체크는 VisitClinicalStatus 기준이며,
     *       FinalOrder.status는 별도 도메인 상태임.
     *       프론트는 VisitClinicalStatus = FINAL_ORDER_READY 일 때만 이 API를 호출해야 함.
     */
    @Transactional
    public FinalOrderResponse finalizeOrder(Long id) {
        FinalOrder o = repo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "FinalOrder not found: " + id));

        String before = (o.getStatus() == null ? "" : o.getStatus().toUpperCase());

        if ("FINALIZED".equals(before)) return toResponse(o);

        if (!"ORDERED".equals(before)) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Only ORDERED finalOrder can be finalized. status=" + o.getStatus());
        }

        o.setStatus("FINALIZED");
        o.setUpdatedAt(LocalDateTime.now());

        // FINAL_ORDER_FINALIZED 이벤트 Outbox 기록
        Map<String, Object> finalizePayload = buildFinalizePayload(o);
        outbox.record("FINAL_ORDER_FINALIZED", "FINAL_ORDER", String.valueOf(id),
                String.valueOf(id), topicFinalOrder, finalizePayload);

        audit.write("FINAL_ORDER_FINALIZED", "FINAL_ORDER", String.valueOf(id), null,
                Map.of("finalOrderId", id, "visitId", o.getVisitId(), "type", o.getType()));

        // Phase 2: VisitClinicalStatus → FINAL_ORDER_CONFIRMED
        try {
            visitStatusSvc.markFinalOrderConfirmed(o.getVisitId());
            log.info("[Phase2] VisitClinicalStatus FINAL_ORDER_CONFIRMED. visitId={}", o.getVisitId());
        } catch (Exception e) {
            log.warn("[Phase2] markFinalOrderConfirmed 실패. visitId={}", o.getVisitId(), e);
        }

        return toResponse(o);
    }

    @Transactional(readOnly = true)
    public FinalOrderResponse get(Long id) {
        FinalOrder o = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("FinalOrder not found: " + id));
        return toResponse(o);
    }

    // ─── Finalize payload 구성 ────────────────────────────────

    private Map<String, Object> buildFinalizePayload(FinalOrder o) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("finalOrderId", o.getFinalOrderId());
        payload.put("visitId", o.getVisitId());
        payload.put("type", o.getType());
        payload.put("status", o.getStatus());
        payload.put("note", o.getNote());

        Map<String, String> parsed = parseDirectivePairs(o.getNote());
        putIfPresent(payload, "ward", parsed.get("ward"));
        putIfPresent(payload, "surgeryName", parsed.get("surgeryname"));
        putIfPresent(payload, "room", parsed.get("room"));
        putIfPresent(payload, "execNote", parsed.get("execnote"));
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) payload.put(key, value.trim());
    }

    private Map<String, String> parseDirectivePairs(String note) {
        Map<String, String> out = new LinkedHashMap<>();
        if (note == null || note.isBlank()) return out;
        String normalized = note.replace('\n', ';').replace(',', ';');
        for (String token : normalized.split(";")) {
            if (token == null) continue;
            String t = token.trim();
            if (t.isBlank() || !t.contains("=")) continue;
            String[] arr = t.split("=", 2);
            String k = arr[0].trim().toLowerCase();
            String v = arr.length > 1 ? arr[1].trim() : "";
            if (!k.isBlank()) out.put(k, v);
        }
        return out;
    }

    private FinalOrderResponse toResponse(FinalOrder o) {
        return FinalOrderResponse.builder()
                .finalOrderId(o.getFinalOrderId())
                .visitId(o.getVisitId())
                .type(o.getType())
                .status(o.getStatus())
                .note(o.getNote())
                .idempotencyKey(o.getIdempotencyKey())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
