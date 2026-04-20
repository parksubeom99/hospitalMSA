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
 * Phase 2-C: AdminMaster → Clinical VISIT_REGISTERED 이벤트 수신
 *
 * 토픽: his.adminmaster.visit.registered
 * 처리: visitId 기준으로 visit_clinical_status INSERT (WAITING 상태)
 *       → 진료 화면 드롭다운에 해당 환자 자동 등록
 *
 * 멱등성: ProcessedEventRepository — eventId 기준 중복 처리 차단
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisitRegisteredConsumer {

    private final VisitClinicalStatusService visitStatusSvc;
    private final ProcessedEventRepository processedRepo;
    private final ObjectMapper om = new ObjectMapper();

    @KafkaListener(
            topics = "${kafka.topic.his-visit-registered:his.adminmaster.visit.registered}",
            groupId = "${spring.kafka.consumer.group-id:clinical-service}"
    )
    @Transactional
    public void onVisitRegistered(String message) throws Exception {
        JsonNode root = om.readTree(message);

        // 멱등성 체크 — eventId 기준
        String eventId = root.path("eventId").asText();
        if (eventId == null || eventId.isBlank()) {
            log.warn("[VisitRegistered] eventId 없음 — 메시지 무시");
            return;
        }
        if (processedRepo.existsByEventId(eventId)) {
            log.debug("[VisitRegistered] 중복 이벤트 무시. eventId={}", eventId);
            return;
        }

        // 이벤트 타입 검증
        String eventType = root.path("eventType").asText();
        if (!"VISIT_REGISTERED".equalsIgnoreCase(eventType)) {
            processedRepo.save(ProcessedEvent.builder()
                    .eventId(eventId).processedAt(LocalDateTime.now()).build());
            return;
        }

        // 페이로드 파싱
        JsonNode payload = root.path("payload");
        long visitId = payload.path("visitId").asLong();
        String patientName = payload.path("patientName").asText("");

        if (visitId <= 0) {
            log.warn("[VisitRegistered] 유효하지 않은 visitId={}. eventId={}", visitId, eventId);
            processedRepo.save(ProcessedEvent.builder()
                    .eventId(eventId).processedAt(LocalDateTime.now()).build());
            return;
        }

        // visit_clinical_status SOAP_IN_PROGRESS 상태로 초기화 (이미 존재하면 그대로 반환)
        // patientName을 함께 저장하여 진료화면 드롭다운에서 환자명 표시 지원
        visitStatusSvc.initOrGet(visitId, patientName);

        // 멱등성 기록
        processedRepo.save(ProcessedEvent.builder()
                .eventId(eventId).processedAt(LocalDateTime.now()).build());

        log.info("[Phase2-C] VisitRegistered 처리 완료. visitId={}, patientName={} → visit_clinical_status WAITING",
                visitId, patientName);
    }
}
