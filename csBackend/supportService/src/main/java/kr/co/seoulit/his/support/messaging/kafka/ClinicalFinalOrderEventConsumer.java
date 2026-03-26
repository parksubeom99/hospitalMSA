package kr.co.seoulit.his.support.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.support.domain.execution.dto.CreateInjectionTaskRequest;
import kr.co.seoulit.his.support.domain.execution.dto.CreateMedExecTaskRequest;
import kr.co.seoulit.his.support.domain.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClinicalFinalOrderEventConsumer {

    private final ExecutionService executionService;
    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper om = new ObjectMapper();

    @KafkaListener(topics = "${kafka.topic.clinical-finalorder:clinical.finalorder.v1}")
    @Transactional
    public void onMessage(String message) throws Exception {
        JsonNode root = om.readTree(message);

        String eventId = root.path("eventId").asText();
        if (eventId == null || eventId.isBlank()) return;
        if (processedRepo.existsByEventId(eventId)) return;

        String eventType = root.path("eventType").asText();
        if (!"FINAL_ORDER_FINALIZED".equalsIgnoreCase(eventType)) {
            processedRepo.save(ProcessedEvent.builder().eventId(eventId).processedAt(LocalDateTime.now()).build());
            return;
        }

        JsonNode payload = root.path("payload");
        long finalOrderId = payload.path("finalOrderId").asLong();
        String type = payload.path("type").asText(); // INJECTION / MED / ...

        if ("INJECTION".equalsIgnoreCase(type)) {
            CreateInjectionTaskRequest req = new CreateInjectionTaskRequest();
            req.setFinalOrderId(finalOrderId);
            String execNote = payload.path("execNote").asText(null);
            String note = payload.path("note").asText(null);
            req.setNote((execNote != null && !execNote.isBlank()) ? execNote : ((note != null && !note.isBlank()) ? note : "AUTO(KAFKA): FINAL_ORDER_FINALIZED"));
            req.setIdempotencyKey("KAFKA-" + eventId);
            executionService.createInjectionTask(req);
        } else if ("MED".equalsIgnoreCase(type)) {
            CreateMedExecTaskRequest req = new CreateMedExecTaskRequest();
            req.setFinalOrderId(finalOrderId);
            String execNote2 = payload.path("execNote").asText(null);
            String note2 = payload.path("note").asText(null);
            req.setNote((execNote2 != null && !execNote2.isBlank()) ? execNote2 : ((note2 != null && !note2.isBlank()) ? note2 : "AUTO(KAFKA): FINAL_ORDER_FINALIZED"));
            req.setIdempotencyKey("KAFKA-" + eventId);
            executionService.createMedExecTask(req);
        } else {
            // Support 책임 범위 아님(ADT/SURGERY 등)
        }

        processedRepo.save(ProcessedEvent.builder().eventId(eventId).processedAt(LocalDateTime.now()).build());
        log.info("Consumed FINAL_ORDER_FINALIZED: eventId={}, finalOrderId={}, type={}", eventId, finalOrderId, type);
    }
}
