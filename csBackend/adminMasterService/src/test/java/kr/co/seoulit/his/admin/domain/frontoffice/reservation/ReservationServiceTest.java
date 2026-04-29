package kr.co.seoulit.his.admin.domain.frontoffice.reservation;

import kr.co.seoulit.his.admin.audit.AuditClient;
import kr.co.seoulit.his.admin.domain.frontoffice.reservation.dto.ReservationResponse;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.VisitService;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitResponse;
import kr.co.seoulit.his.admin.domain.master.patient.Patient;
import kr.co.seoulit.his.admin.domain.master.patient.PatientRepository;
import kr.co.seoulit.his.admin.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * ReservationService.checkIn 회귀 테스트
 *
 * 발표 직전 버그 회귀 방지 (수정 커밋: 03d50d6 / BFG 후 신 SHA):
 *  - 증상: 예약내원 접수 시 admin_visit은 생성되지만 admin_reservation.status가 BOOKED 잔류
 *  - 결과: 예약 현황에 잔류 → 중복 표시 버그
 *  - 수정: ReservationService.checkIn이 Visit 생성 + status=CHECKED_IN + visitId 매핑 동시 처리
 *
 * 검증 항목:
 *  ① BOOKED → checkIn → Visit 생성 + status=CHECKED_IN + visitId 매핑 (ArgumentCaptor 2개)
 *  ② 이미 CHECKED_IN → BusinessException 발생 + 부작용 모두 never
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService.checkIn 회귀 테스트")
class ReservationServiceTest {

    @Mock ReservationRepository reservations;
    @Mock PatientRepository patients;
    @Mock VisitService visits;
    @Mock AuditClient audit;

    @InjectMocks
    ReservationService reservationService;

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ①: BOOKED → checkIn → CHECKED_IN 전환 + Visit 매핑 (회귀 방지 핵심)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("BOOKED 예약 checkIn 호출 시 Visit 생성 + reservation.status=CHECKED_IN + visitId 매핑이 함께 발생한다 (commit 03d50d6 회귀 방지)")
    void checkIn_BookedReservation_ShouldCreateVisitAndTransitionStatus() {
        // given — BOOKED 상태 예약
        Reservation booked = Reservation.builder()
                .reservationId(21001L)
                .patientId(1001L)
                .patientName("홍길동")
                .departmentCode("IM")
                .doctorId("DOC01")
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .status("BOOKED")
                .createdAt(LocalDateTime.now())
                .build();
        given(reservations.findById(21001L)).willReturn(Optional.of(booked));

        // VisitResponse — record 16 fields, 단위 테스트에선 visitId만 의미
        VisitResponse mockVisit = new VisitResponse(
                11001L,                                  // visitId
                1001L,                                   // patientId
                "홍길동",                                  // patientName
                null, null, null,                        // gender, rrnMasked, patientPhone
                "IM", "DOC01", "WAITING",                // departmentCode, doctorId, status
                null, null,                              // arrivalType, triageLevel
                LocalDateTime.now(), LocalDateTime.now(),// createdAt, updatedAt
                null, null, null                         // canceledAt, cancelReason, closedAt
        );
        given(visits.create(any(VisitCreateRequest.class))).willReturn(mockVisit);

        given(reservations.save(any(Reservation.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // toResponse 내부 patients.findById 호출 대비
        given(patients.findById(1001L)).willReturn(Optional.of(
                Patient.builder()
                        .patientId(1001L)
                        .name("홍길동")
                        .phone("010-1234-5678")
                        .build()
        ));

        // when
        ReservationResponse result = reservationService.checkIn(21001L);

        // then ① — VisitCreateRequest 인자 검증 (예약 → Visit 데이터 전이)
        ArgumentCaptor<VisitCreateRequest> visitReqCap = ArgumentCaptor.forClass(VisitCreateRequest.class);
        verify(visits, times(1)).create(visitReqCap.capture());
        VisitCreateRequest visitReq = visitReqCap.getValue();
        assertThat(visitReq.patientId()).isEqualTo(1001L);
        assertThat(visitReq.patientName()).isEqualTo("홍길동");
        assertThat(visitReq.departmentCode()).isEqualTo("IM");
        assertThat(visitReq.doctorId()).isEqualTo("DOC01");
        assertThat(visitReq.arrivalType()).isEqualTo("RESERVATION");
        assertThat(visitReq.triageLevel()).isNull();

        // then ② — Reservation save 시 status=CHECKED_IN + visitId 매핑 (회귀 방지 핵심 ⭐)
        ArgumentCaptor<Reservation> savedCap = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations, times(1)).save(savedCap.capture());
        Reservation saved = savedCap.getValue();
        assertThat(saved.getStatus()).isEqualTo("CHECKED_IN");
        assertThat(saved.getVisitId()).isEqualTo(11001L);

        // then ③ — Audit 기록 1회 (patientId 자리 null)
        verify(audit, times(1)).write(
                eq("RESERVATION_CHECKED_IN"),
                eq("RESERVATION"),
                eq("21001"),
                isNull(),
                anyMap()
        );

        // then ④ — 반환 응답에도 전이된 상태 노출
        assertThat(result.status()).isEqualTo("CHECKED_IN");
        assertThat(result.visitId()).isEqualTo(11001L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 테스트 ②: 이미 CHECKED_IN → checkIn → BusinessException + 부작용 never
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("이미 CHECKED_IN 상태 예약에 checkIn 호출 시 BusinessException이 발생하고 Visit 생성/save/audit 모두 일어나지 않는다")
    void checkIn_AlreadyCheckedIn_ShouldThrowAndProduceNoSideEffects() {
        // given — 이미 CHECKED_IN 상태 예약
        Reservation already = Reservation.builder()
                .reservationId(21001L)
                .patientId(1001L)
                .patientName("홍길동")
                .departmentCode("IM")
                .doctorId("DOC01")
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .status("CHECKED_IN")
                .visitId(11001L)
                .createdAt(LocalDateTime.now())
                .build();
        given(reservations.findById(21001L)).willReturn(Optional.of(already));

        // when / then — BusinessException 발생 + 메시지 검증 (VisitServiceTest 패턴)
        assertThatThrownBy(() -> reservationService.checkIn(21001L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only BOOKED reservation can be checked in");

        // then — 부작용 모두 발생 X (도메인 무결성)
        verify(visits, never()).create(any());
        verify(reservations, never()).save(any());
        verify(audit, never()).write(anyString(), anyString(), anyString(), any(), anyMap());
    }
}
