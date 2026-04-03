package kr.co.seoulit.his.clinical.domain.order.service;

import kr.co.seoulit.his.clinical.audit.AuditClient;
import kr.co.seoulit.his.clinical.domain.emr.soap.SoapNoteRepository;
import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import kr.co.seoulit.his.clinical.integration.support.client.SupportWorklistClient;
import kr.co.seoulit.his.clinical.messaging.outbox.OutboxService;
import kr.co.seoulit.his.clinical.global.response.CurrentUserUtil;
import kr.co.seoulit.his.clinical.global.exception.BusinessException;
import kr.co.seoulit.his.clinical.global.exception.ErrorCode;
import kr.co.seoulit.his.clinical.domain.order.OrderHeader;
import kr.co.seoulit.his.clinical.domain.order.OrderItem;
import kr.co.seoulit.his.clinical.domain.order.OrderItemRepository;
import kr.co.seoulit.his.clinical.domain.order.OrderRepository;
import kr.co.seoulit.his.clinical.domain.order.dto.CreateOrderRequest;
import kr.co.seoulit.his.clinical.domain.order.dto.OrderDeleteRequest;
import kr.co.seoulit.his.clinical.domain.order.dto.OrderItemResponse;
import kr.co.seoulit.his.clinical.domain.order.dto.OrderResponse;
import kr.co.seoulit.his.clinical.domain.order.dto.UpdateOrderItemsRequest;
import kr.co.seoulit.his.clinical.domain.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final OrderMapper mapper;
    private final AuditClient audit;
    private final SupportWorklistClient supportWorklist;

    // Phase 2
    private final VisitClinicalStatusService visitStatusSvc;
    private final OutboxService outbox;
    private final SoapNoteRepository soapNoteRepo;

    @Value("${kafka.topic.his-diagnostic-order-submitted:his.clinical.diagnostic-order.submitted}")
    private String topicDiagnosticSubmitted;

    private OrderHeader getActiveOrderOrThrow(Long orderId) {
        return orders.findByOrderIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found. orderId=" + orderId));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
        return orders.findByIdempotencyKeyAndDeletedFalse(req.idempotencyKey())
                .map(mapper::toResponse)
                .orElseGet(() -> {
                    String primaryCode = (req.items() != null && !req.items().isEmpty()) ? req.items().get(0).itemCode() : null;
                    String primaryName = (req.items() != null && !req.items().isEmpty()) ? req.items().get(0).itemName() : null;

                    OrderHeader o = OrderHeader.builder()
                            .visitId(req.visitId())
                            .category(req.category())
                            .primaryItemCode(primaryCode)
                            .primaryItemName(primaryName)
                            .status("NEW")
                            .idempotencyKey(req.idempotencyKey())
                            .createdAt(LocalDateTime.now())
                            .build();
                    OrderHeader saved = orders.save(o);

                    if (req.items() != null) {
                        for (var it : req.items()) {
                            items.save(OrderItem.builder()
                                    .orderId(saved.getOrderId())
                                    .itemCode(it.itemCode())
                                    .itemName(it.itemName())
                                    .quantity(it.qty())
                                    .build());
                        }
                    }

                    audit.write("ORDER_CREATED", "ORDER", String.valueOf(saved.getOrderId()), null,
                            Map.of("status", saved.getStatus(), "visitId", saved.getVisitId(), "category", saved.getCategory()));

                    // Support Worklist 자동 생성 (REST, 실패 시 degrade)
                    supportWorklist.createWorklistTask(
                            saved.getOrderId(), saved.getVisitId(), saved.getCategory(),
                            saved.getStatus(), saved.getPrimaryItemCode(), saved.getPrimaryItemName()
                    );

                    // Phase 2: 검사오더 저장 후 SOAP 존재하면 DIAGNOSTIC_ORDER_SUBMITTED 발행
                    tryAdvanceToDiagnosticSubmitted(saved.getVisitId());

                    return mapper.toResponse(saved);
                });
    }

    /**
     * Phase 2: 검사오더 저장 후 SOAP이 이미 작성됐으면 EXAM_REQUESTED 전환 및
     * DIAGNOSTIC_ORDER_SUBMITTED 이벤트 발행
     *
     * SoapNoteService.upsert()와 함께 양방향 트리거 구조:
     * - SOAP 먼저 → 검사오더 저장 시 여기서 발행
     * - 검사오더 먼저 → SOAP 저장 시 SoapNoteService에서 발행
     * - visitStatusSvc.markExamRequested()가 false 반환 = 이미 발행됨 → 무시 (idempotent)
     * [MODIFIED] record → recordIfAbsent: Outbox 레벨 중복 발행 방지 추가
     */
    private void tryAdvanceToDiagnosticSubmitted(Long visitId) {
        try {
            boolean soapExists = soapNoteRepo.existsById(visitId);
            if (!soapExists) return; // SOAP 아직 없음 → SoapNoteService에서 발행

            boolean transitioned = visitStatusSvc.markExamRequested(visitId);
            if (transitioned) {
                visitStatusSvc.markExamInProgress(visitId);
                // [MODIFIED] record → recordIfAbsent: 동일 visitId 중복 발행 차단
                outbox.recordIfAbsent(
                        "DIAGNOSTIC_ORDER_SUBMITTED",
                        "VISIT",
                        String.valueOf(visitId),
                        String.valueOf(visitId), // partitionKey = visitId
                        topicDiagnosticSubmitted,
                        Map.of("visitId", visitId, "triggeredBy", "EXAM_ORDER_SAVED")
                );
                log.info("[Phase2] DIAGNOSTIC_ORDER_SUBMITTED published. visitId={}", visitId);
            }
        } catch (Exception e) {
            log.warn("[Phase2] Failed to advance diagnostic status. visitId={}", visitId, e);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId) {
        return mapper.toResponse(getActiveOrderOrThrow(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(String status, String category) {
        List<OrderHeader> list;
        if (status != null && category != null) list = orders.findByStatusAndCategoryAndDeletedFalse(status, category);
        else if (status != null) list = orders.findByStatusAndDeletedFalse(status);
        else list = orders.findAllByDeletedFalse();
        return mapper.toResponseList(list);
    }

    @Transactional
    public OrderResponse updateItems(Long orderId, UpdateOrderItemsRequest req) {
        OrderHeader o = getActiveOrderOrThrow(orderId);

        if (!"NEW".equalsIgnoreCase(o.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only NEW order can be updated. status=" + o.getStatus());
        }

        items.deleteByOrderId(orderId);
        for (var it : req.items()) {
            items.save(OrderItem.builder()
                    .orderId(orderId)
                    .itemCode(it.itemCode())
                    .itemName(it.itemName())
                    .quantity(it.qty())
                    .build());
        }

        o.setUpdatedAt(LocalDateTime.now());
        orders.save(o);

        audit.write("ORDER_UPDATED", "ORDER", String.valueOf(o.getOrderId()), null,
                Map.of("status", o.getStatus()));

        return mapper.toResponse(o);
    }

    @Transactional
    public OrderResponse cancel(Long orderId) {
        OrderHeader o = getActiveOrderOrThrow(orderId);
        if (!"NEW".equalsIgnoreCase(o.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only NEW order can be canceled. status=" + o.getStatus());
        }
        o.setStatus("CANCELED");
        o.setUpdatedAt(LocalDateTime.now());
        OrderHeader saved = orders.save(o);
        audit.write("ORDER_CANCELED", "ORDER", String.valueOf(saved.getOrderId()), null, Map.of("status", saved.getStatus()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse markInProgress(Long orderId) {
        OrderHeader o = getActiveOrderOrThrow(orderId);
        if ("DONE".equalsIgnoreCase(o.getStatus()))
            throw new BusinessException(ErrorCode.INVALID_STATE, "DONE order cannot be marked IN_PROGRESS.");
        if ("CANCELED".equalsIgnoreCase(o.getStatus()))
            throw new BusinessException(ErrorCode.INVALID_STATE, "CANCELED order cannot be marked IN_PROGRESS.");
        o.setStatus("IN_PROGRESS");
        o.setUpdatedAt(LocalDateTime.now());
        OrderHeader saved = orders.save(o);
        audit.write("ORDER_IN_PROGRESS", "ORDER", String.valueOf(saved.getOrderId()), null, Map.of("status", saved.getStatus()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse markResulted(Long orderId) {
        OrderHeader o = getActiveOrderOrThrow(orderId);
        if ("CANCELED".equalsIgnoreCase(o.getStatus()))
            throw new BusinessException(ErrorCode.INVALID_STATE, "CANCELED order cannot be marked RESULTED.");
        if (!("NEW".equalsIgnoreCase(o.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(o.getStatus())))
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only NEW/IN_PROGRESS order can be marked RESULTED. status=" + o.getStatus());
        o.setStatus("DONE");
        o.setUpdatedAt(LocalDateTime.now());
        OrderHeader saved = orders.save(o);
        audit.write("ORDER_RESULTED", "ORDER", String.valueOf(saved.getOrderId()), null, Map.of("status", saved.getStatus()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse markReviewed(Long orderId) {
        OrderHeader o = getActiveOrderOrThrow(orderId);
        if ("CANCELED".equalsIgnoreCase(o.getStatus()))
            throw new BusinessException(ErrorCode.INVALID_STATE, "CANCELED order cannot be marked REVIEWED.");
        if (!("DONE".equalsIgnoreCase(o.getStatus()) || "RESULTED".equalsIgnoreCase(o.getStatus())))
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only DONE/RESULTED order can be marked REVIEWED. status=" + o.getStatus());
        o.setStatus("REVIEWED");
        o.setUpdatedAt(LocalDateTime.now());
        OrderHeader saved = orders.save(o);
        audit.write("ORDER_REVIEWED", "ORDER", String.valueOf(saved.getOrderId()), null, Map.of("status", saved.getStatus()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse markDone(Long orderId) {
        OrderHeader o = getActiveOrderOrThrow(orderId);
        if ("CANCELED".equalsIgnoreCase(o.getStatus()))
            throw new BusinessException(ErrorCode.INVALID_STATE, "CANCELED order cannot be marked DONE.");
        o.setStatus("DONE");
        o.setUpdatedAt(LocalDateTime.now());
        OrderHeader saved = orders.save(o);
        audit.write("ORDER_DONE", "ORDER", String.valueOf(saved.getOrderId()), null, Map.of("status", saved.getStatus()));
        return mapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse delete(Long orderId, OrderDeleteRequest req) {
        OrderHeader o = getActiveOrderOrThrow(orderId);
        if (!"NEW".equalsIgnoreCase(o.getStatus()) && !"CANCELED".equalsIgnoreCase(o.getStatus()))
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only NEW/CANCELED order can be deleted. status=" + o.getStatus());
        o.setDeleted(true);
        o.setDeletedAt(LocalDateTime.now());
        o.setDeletedBy(CurrentUserUtil.currentLoginIdOrNull());
        o.setDeletedReason(req == null ? null : req.reason());
        o.setUpdatedAt(LocalDateTime.now());
        OrderHeader saved = orders.save(o);
        audit.write("ORDER_DELETED", "ORDER", String.valueOf(saved.getOrderId()), null, Map.of("status", saved.getStatus(), "deleted", true));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> listItems(Long orderId) {
        getActiveOrderOrThrow(orderId);
        return mapper.toItemResponseList(items.findByOrderId(orderId));
    }
}
