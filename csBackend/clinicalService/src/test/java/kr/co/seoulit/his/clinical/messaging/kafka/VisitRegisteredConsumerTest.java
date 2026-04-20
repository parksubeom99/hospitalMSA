package kr.co.seoulit.his.clinical.messaging.kafka;

import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatus;
import kr.co.seoulit.his.clinical.domain.visitstatus.VisitClinicalStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * VisitRegisteredConsumer 단위 테스트
 *
 * 검증 항목:
 *  5. 정상 VISIT_REGISTERED 이벤트 수신 시 visit_clinical_status WAITING 생성
 *  6. 동일 eventId 중복 수신 시 처리 무시 (멱등성 보장)
 *  7. 유효하지 않은 visitId(0) 수신 시 처리 건너뜀
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VisitRegisteredConsumer 단위 테스트")
class VisitRegisteredConsumerTest {

    @Mock VisitClinicalStatusService visitStatusSvc;
    @Mock ProcessedEventRepository processedRepo;

    @InjectMocks
    VisitRegisteredConsumer consumer;

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 5: 정상 VISIT_REGISTERED 수신 시 visit_clinical_status 생성
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("정상 VISIT_REGISTERED 이벤트 수신 시 visit_clinical_status가 WAITING으로 초기화된다")
    void onVisitRegistered_Normal_ShouldInitClinicalStatus() throws Exception {
        // given
        String message = """
                {
                  "eventId": "evt-001",
                  "eventType": "VISIT_REGISTERED",
                  "payload": {
                    "visitId": 11001,
                    "patientId": 1001,
                    "patientName": "홍길동",
                    "status": "WAITING"
                  }
                }
                """;

        given(processedRepo.existsByEventId("evt-001")).willReturn(false);

        VisitClinicalStatus mockStatus = VisitClinicalStatus.builder()
                .visitId(11001L)
                .clinicalStatus("WAITING")
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        // [MODIFIED v3.3] initOrGet 오버로드 2인자 시그니처로 변경
        // Consumer가 Kafka 이벤트의 patientName을 함께 전달하도록 확장됨
        given(visitStatusSvc.initOrGet(11001L, "홍길동")).willReturn(mockStatus);
        given(processedRepo.save(any())).willReturn(null);

        // when
        consumer.onVisitRegistered(message);

        // then — visit_clinical_status 초기화 1회 호출 확인 (visitId + patientName)
        verify(visitStatusSvc, times(1)).initOrGet(11001L, "홍길동");
        verify(processedRepo, times(1)).save(argThat(e -> "evt-001".equals(e.getEventId())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 6: 동일 eventId 중복 수신 시 멱등성 보장 (처리 무시)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("이미 처리된 eventId로 중복 수신 시 visit_clinical_status 초기화를 건너뛴다 (멱등성)")
    void onVisitRegistered_DuplicateEvent_ShouldBeIgnored() throws Exception {
        // given — 이미 처리된 이벤트
        String message = """
                {
                  "eventId": "evt-001",
                  "eventType": "VISIT_REGISTERED",
                  "payload": {
                    "visitId": 11001,
                    "patientId": 1001,
                    "patientName": "홍길동",
                    "status": "WAITING"
                  }
                }
                """;

        given(processedRepo.existsByEventId("evt-001")).willReturn(true); // 이미 처리됨

        // when
        consumer.onVisitRegistered(message);

        // then — 중복이므로 initOrGet 호출 안 됨 (2인자 시그니처 기준)
        verify(visitStatusSvc, never()).initOrGet(anyLong(), anyString());
        verify(processedRepo, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 7: visitId = 0 (유효하지 않은 값) 수신 시 처리 건너뜀
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("visitId가 0 이하인 이벤트 수신 시 visit_clinical_status 초기화를 건너뛴다")
    void onVisitRegistered_InvalidVisitId_ShouldSkip() throws Exception {
        // given
        String message = """
                {
                  "eventId": "evt-bad",
                  "eventType": "VISIT_REGISTERED",
                  "payload": {
                    "visitId": 0,
                    "patientId": 0,
                    "patientName": "",
                    "status": "WAITING"
                  }
                }
                """;

        given(processedRepo.existsByEventId("evt-bad")).willReturn(false);
        given(processedRepo.save(any())).willReturn(null);

        // when
        consumer.onVisitRegistered(message);

        // then — visitId 유효하지 않으므로 initOrGet 호출 안 됨 (2인자 시그니처 기준)
        verify(visitStatusSvc, never()).initOrGet(anyLong(), anyString());
    }
}
