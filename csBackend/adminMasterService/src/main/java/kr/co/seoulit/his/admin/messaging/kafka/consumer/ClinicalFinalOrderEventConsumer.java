package kr.co.seoulit.his.admin.messaging.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.admin.domain.execution.dto.CreateSurgeryTaskRequest;
import kr.co.seoulit.his.admin.domain.execution.dto.RecordAdmissionRequest;
import kr.co.seoulit.his.admin.domain.execution.service.AdminExecutionService;
import kr.co.seoulit.his.admin.messaging.kafka.ProcessedEvent;
import kr.co.seoulit.his.admin.messaging.kafka.ProcessedEventRepository;
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

    private final AdminExecutionService adminExecutionService;
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
        String type = payload.path("type").asText(); // ADMISSION / SURGERY / ...

        if ("ADMISSION".equalsIgnoreCase(type)) {
            RecordAdmissionRequest req = new RecordAdmissionRequest();
            req.setFinalOrderId(finalOrderId);
            String ward = payload.path("ward").asText(null);
            req.setWard((ward == null || ward.isBlank()) ? "WARD-1" : ward); // payload 우선, 없으면 기본값
            req.setIdempotencyKey("KAFKA-" + eventId);
            adminExecutionService.createAdmissionTask(req);
        } else if ("SURGERY".equalsIgnoreCase(type)) {
            CreateSurgeryTaskRequest req = new CreateSurgeryTaskRequest();
            req.setFinalOrderId(finalOrderId);
            String surgeryName = payload.path("surgeryName").asText(null);
            req.setSurgeryName((surgeryName == null || surgeryName.isBlank()) ? "SURGERY" : surgeryName);
            req.setRoom(payload.path("room").asText(null));
            req.setIdempotencyKey("KAFKA-" + eventId);
            adminExecutionService.createSurgeryTask(req);
        } else {
            // Admin 책임 범위 아님(MED/INJECTION 등)
        }

        processedRepo.save(ProcessedEvent.builder().eventId(eventId).processedAt(LocalDateTime.now()).build());
        log.info("Consumed FINAL_ORDER_FINALIZED: eventId={}, finalOrderId={}, type={}", eventId, finalOrderId, type);
    }
}
