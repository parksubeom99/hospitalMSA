package kr.co.seoulit.his.support.domain.worklist.service;

import kr.co.seoulit.his.support.audit.AuditClient;
import kr.co.seoulit.his.support.integration.clinical.client.OrderClient;
import kr.co.seoulit.his.support.integration.clinical.dto.OrderDto;
import kr.co.seoulit.his.support.messaging.outbox.OutboxService;
import kr.co.seoulit.his.support.messaging.outbox.QueueEventOutboxService;
import kr.co.seoulit.his.support.domain.worklist.WorklistTask;
import kr.co.seoulit.his.support.domain.worklist.WorklistTaskRepository;
import kr.co.seoulit.his.support.domain.worklist.dto.WorkItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorklistService {

    private final OrderClient orderClient;
    private final QueueEventOutboxService outbox;
    private final OutboxService diagnosticOutbox;       // Phase 2: DIAGNOSTIC_ORDER_COMPLETED 발행용
    private final WorklistTaskRepository tasks;
    private final AuditClient audit;

    @Value("${kafka.topic.his-diagnostic-order-completed:his.support.diagnostic-order.completed}")
    private String topicDiagnosticCompleted;

    /**
     * Worklist 조회
     */
    public List<WorkItemDto> getWorklist(String category, String status, String primaryItemCode) {
        String effectiveStatus = (status == null || status.isBlank()) ? "NEW" : status;

        try {
            List<WorklistTask> persisted = (primaryItemCode == null || primaryItemCode.isBlank())
                    ? tasks.findByCategoryAndStatusOrderByCreatedAtDesc(category, effectiveStatus)
                    : tasks.findByCategoryAndStatusAndPrimaryItemCodeOrderByCreatedAtDesc(category, effectiveStatus, primaryItemCode);

            if (!persisted.isEmpty()) {
                return persisted.stream()
                        .map(t -> new WorkItemDto(
                                t.getOrderId(), t.getVisitId(), t.getCategory(),
                                t.getStatus(), t.getPrimaryItemCode(), t.getPrimaryItemName()
                        ))
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {}

        // 호환/복구: Clinical에서 조회
        List<OrderDto> orders = orderClient.fetchOrders(effectiveStatus, category);
        for (var o : orders) {
            try {
                tasks.findByOrderId(o.orderId()).ifPresentOrElse(existing -> {
                    existing.setStatus(o.status());
                    existing.setPrimaryItemCode(o.primaryItemCode());
                    existing.setPrimaryItemName(o.primaryItemName());
                    existing.setUpdatedAt(java.time.LocalDateTime.now());
                    tasks.save(existing);
                }, () -> tasks.save(WorklistTask.builder()
                        .orderId(o.orderId()).visitId(o.visitId()).category(o.category())
                        .primaryItemCode(o.primaryItemCode()).primaryItemName(o.primaryItemName())
                        .status(o.status()).createdAt(java.time.LocalDateTime.now()).build()));
            } catch (Exception ignored) {}
        }

        return orders.stream()
                .map(o -> new WorkItemDto(o.orderId(), o.visitId(), o.category(), o.status(), o.primaryItemCode(), o.primaryItemName()))
                .collect(Collectors.toList());
    }

    /**
     * 담당자 착수
     */
    public void startWork(Long orderId, String category) {
        orderClient.markInProgress(orderId);

        try {
            tasks.findByOrderId(orderId).ifPresent(t -> {
                t.setStatus("IN_PROGRESS");
                t.setUpdatedAt(java.time.LocalDateTime.now());
                tasks.save(t);
            });
        } catch (Exception ignored) {}

        String effectiveCategory = (category == null || category.isBlank()) ? "UNKNOWN" : category;
        outbox.enqueue("QUEUE_CALLED", orderId, effectiveCategory, null);
        audit.write("WORKLIST_STARTED", "WORKLIST_TASK", String.valueOf(orderId), null, Map.of("category", effectiveCategory));
    }

    /**
     * Worklist task upsert (Clinical → Support 자동 생성)
     */
    public WorklistTask upsertTask(Long orderId, Long visitId, String category, String status,
                                   String primaryItemCode, String primaryItemName) {
        String effectiveStatus = (status == null || status.isBlank()) ? "NEW" : status;
        try {
            return tasks.findByOrderId(orderId)
                    .map(existing -> {
                        existing.setVisitId(visitId);
                        existing.setCategory(category);
                        existing.setStatus(effectiveStatus);
                        existing.setPrimaryItemCode(primaryItemCode);
                        existing.setPrimaryItemName(primaryItemName);
                        existing.setUpdatedAt(java.time.LocalDateTime.now());
                        return tasks.save(existing);
                    })
                    .orElseGet(() -> tasks.save(WorklistTask.builder()
                            .orderId(orderId).visitId(visitId).category(category)
                            .primaryItemCode(primaryItemCode).primaryItemName(primaryItemName)
                            .status(effectiveStatus).createdAt(java.time.LocalDateTime.now()).build()));
        } catch (Exception e) {
            return WorklistTask.builder()
                    .orderId(orderId).visitId(visitId).category(category)
                    .primaryItemCode(primaryItemCode).primaryItemName(primaryItemName)
                    .status(effectiveStatus).createdAt(java.time.LocalDateTime.now()).build();
        }
    }

    /**
     * Phase 2: 검사 결과 입력 완료 시 Worklist DONE 전이 + DIAGNOSTIC_ORDER_COMPLETED 발행
     *
     * visitId를 파악하기 위해 WorklistTask에서 조회.
     * visitId가 없는 경우(구버전 데이터)에는 이벤트 발행만 스킵, Worklist 완료는 정상 처리.
     */
    public void completeWork(Long orderId, String category) {
        if (orderId == null) return;

        Long visitId = null;
        try {
            var taskOpt = tasks.findByOrderId(orderId);
            if (taskOpt.isPresent()) {
                WorklistTask t = taskOpt.get();
                visitId = t.getVisitId();
                t.setStatus("DONE");
                t.setUpdatedAt(java.time.LocalDateTime.now());
                tasks.save(t);
            }
        } catch (Exception ignored) {}

        String effectiveCategory = (category == null || category.isBlank()) ? "UNKNOWN" : category;

        try {
            audit.write("WORKLIST_COMPLETED", "WORKLIST_TASK", String.valueOf(orderId), null,
                    Map.of("category", effectiveCategory, "status", "DONE"));
        } catch (Exception ignored) {}

        // Phase 2: DIAGNOSTIC_ORDER_COMPLETED 발행 (visitId 있을 때만)
        if (visitId != null) {
            try {
                diagnosticOutbox.record(
                        "DIAGNOSTIC_ORDER_COMPLETED",
                        "WORKLIST_TASK",
                        String.valueOf(orderId),
                        String.valueOf(visitId),
                        topicDiagnosticCompleted,
                        Map.of(
                                "visitId", visitId,
                                "orderId", orderId,
                                "category", effectiveCategory,
                                "status", "DONE"
                        )
                );
                log.info("[Phase2] DIAGNOSTIC_ORDER_COMPLETED published. visitId={}, orderId={}", visitId, orderId);
            } catch (Exception e) {
                log.warn("[Phase2] Failed to publish DIAGNOSTIC_ORDER_COMPLETED. orderId={}", orderId, e);
            }
        }
    }
}
