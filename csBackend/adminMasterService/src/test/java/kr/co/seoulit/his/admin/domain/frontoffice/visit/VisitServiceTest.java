package kr.co.seoulit.his.admin.domain.frontoffice.visit;

import kr.co.seoulit.his.admin.audit.AuditClient;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.invoice.InvoiceRepository;
import kr.co.seoulit.his.admin.domain.frontoffice.queue.QueueService;
import kr.co.seoulit.his.admin.domain.frontoffice.queue.QueueTicket;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitCreateRequest;
import kr.co.seoulit.his.admin.domain.master.patient.PatientRepository;
import kr.co.seoulit.his.admin.global.exception.BusinessException;
import kr.co.seoulit.his.admin.messaging.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisitService 단위 테스트")
class VisitServiceTest {

    @Mock VisitRepository visitRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock QueueService queueService;
    @Mock AuditClient auditClient;
    @Mock OutboxService outboxService;
    // [ADDED v3.3] VisitService.toResponse()가 PatientRepository.findById로 gender/rrnMasked/phone 조회
    // → 테스트에서 Mock 없으면 NullPointerException 발생하여 모든 케이스 실패
    // findById 기본 반환값(Optional.empty())으로도 null 체크 가드가 있어 정상 동작
    @Mock PatientRepository patientRepository;

    @InjectMocks
    VisitService visitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(visitService, "topicVisitRegistered",
                "his.adminmaster.visit.registered");
    }

    @Test
    @DisplayName("접수 생성 시 VISIT_REGISTERED Outbox 이벤트가 발행된다")
    void create_ShouldPublishVisitRegisteredOutboxEvent() {
        // given
        VisitCreateRequest req = new VisitCreateRequest(1001L, "홍길동", "IM", null, "WALK_IN", null);

        Visit savedVisit = Visit.builder()
                .visitId(11001L)
                .patientId(1001L)
                .patientName("홍길동")
                .status("WAITING")
                .createdAt(LocalDateTime.now())
                .build();

        // [FIX] ensureTicket은 QueueTicket 반환 → doNothing() 불가, willReturn 사용
        QueueTicket mockTicket = QueueTicket.builder()
                .visitId(11001L)
                .status("WAITING")
                .build();
        given(visitRepository.save(any(Visit.class))).willReturn(savedVisit);
        given(queueService.ensureTicket(anyLong(), anyString(), anyString())).willReturn(mockTicket);
        doNothing().when(auditClient).write(anyString(), anyString(), anyString(), any(), any());

        // when
        visitService.create(req);

        // then — VISIT_REGISTERED Outbox 이벤트 1회 발행 확인
        verify(outboxService, times(1)).record(
                eq("VISIT_REGISTERED"),
                eq("VISIT"),
                eq("11001"),
                eq("11001"),
                eq("his.adminmaster.visit.registered"),
                argThat(payload ->
                        payload.get("visitId").equals(11001L) &&
                        payload.get("patientName").equals("홍길동")
                )
        );
    }

    @Test
    @DisplayName("WAITING 상태 visit은 정상적으로 취소된다")
    void cancel_WhenWaiting_ShouldSucceed() {
        // given
        Visit waitingVisit = Visit.builder()
                .visitId(11001L)
                .patientId(1001L)
                .status("WAITING")
                .createdAt(LocalDateTime.now())
                .build();

        given(visitRepository.findById(11001L)).willReturn(Optional.of(waitingVisit));
        given(invoiceRepository.existsByVisitIdAndStatus(11001L, "PAID")).willReturn(false);
        given(visitRepository.save(any(Visit.class))).willReturn(waitingVisit);
        doNothing().when(auditClient).write(anyString(), anyString(), anyString(), any(), any());

        // when
        visitService.cancel(11001L, "테스트 취소");

        // then
        assertThat(waitingVisit.getStatus()).isEqualTo("CANCELED");
        verify(visitRepository, times(1)).save(waitingVisit);
    }

    @Test
    @DisplayName("WAITING이 아닌 상태의 visit 취소 시 BusinessException이 발생한다")
    void cancel_WhenNotWaiting_ShouldThrowBusinessException() {
        // given
        Visit inTreatmentVisit = Visit.builder()
                .visitId(11002L)
                .patientId(1001L)
                .status("IN_TREATMENT")
                .createdAt(LocalDateTime.now())
                .build();

        given(visitRepository.findById(11002L)).willReturn(Optional.of(inTreatmentVisit));

        // when & then
        assertThatThrownBy(() -> visitService.cancel(11002L, "잘못된 취소"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only WAITING visit can be canceled");

        verify(outboxService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제 완료(PAID) 인보이스가 있는 visit은 취소할 수 없다")
    void cancel_WhenPaidInvoiceExists_ShouldThrowBusinessException() {
        // given
        Visit waitingVisit = Visit.builder()
                .visitId(11003L)
                .patientId(1001L)
                .status("WAITING")
                .createdAt(LocalDateTime.now())
                .build();

        given(visitRepository.findById(11003L)).willReturn(Optional.of(waitingVisit));
        given(invoiceRepository.existsByVisitIdAndStatus(11003L, "PAID")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> visitService.cancel(11003L, "결제 후 취소 시도"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot cancel. PAID invoice exists.");
    }
}
