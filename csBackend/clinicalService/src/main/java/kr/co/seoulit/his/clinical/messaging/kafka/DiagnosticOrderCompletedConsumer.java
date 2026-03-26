package kr.co.seoulit.his.clinical.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Phase 2: Support → Clinical 검사 완료 이벤트 수신
 *
 * 토픽: his.support.diagnostic-order.completed
 * 처리: visitId의 clinical 상태를 FINAL_ORDER_READY로 전환
 *       → 프론트 오더 화면에서 해당 visitId의 최종오더 액션이 활성화됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosticOrderCompletedConsumer {

    private final VisitClinicalStatusService visitStatusSvc;
    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper om = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.his-diagnostic-order-completed:his.support.diagnostic-order.completed}",
                   groupId = "${spring.kafka.consumer.group-id:clinical-service}")
    @Transactional
    public void onDiagnosticOrderCompleted(String message) throws Exception {
        JsonNode root = om.readTree(message);

        String eventId = root.path("eventId").asText();
        if (eventId == null || eventId.isBlank()) return;
        if (processedRepo.existsByEventId(eventId)) {
            log.debug("[DiagnosticCompleted] 중복 이벤트 무시. eventId={}", eventId);
            return;
        }

        String eventType = root.path("eventType").asText();
        if (!"DIAGNOSTIC_ORDER_COMPLETED".equalsIgnoreCase(eventType)) {
            processedRepo.save(ProcessedEvent.builder()
                    .eventId(eventId).processedAt(LocalDateTime.now()).build());
            return;
        }

        JsonNode payload = root.path("payload");
        long visitId = payload.path("visitId").asLong();
        long orderId = payload.path("orderId").asLong();

        // Visit clinical 상태 → FINAL_ORDER_READY 전환
        visitStatusSvc.markFinalOrderReady(visitId);

        processedRepo.save(ProcessedEvent.builder()
                .eventId(eventId).processedAt(LocalDateTime.now()).build());

        log.info("[Phase2] DiagnosticOrderCompleted 처리 완료. visitId={}, orderId={}, → FINAL_ORDER_READY", visitId, orderId);
    }
}
