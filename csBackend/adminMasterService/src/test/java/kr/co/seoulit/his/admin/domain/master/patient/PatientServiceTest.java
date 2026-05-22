package kr.co.seoulit.his.admin.domain.master.patient;

import kr.co.seoulit.his.admin.audit.MasterAuditClient;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientResolveRequest;
import kr.co.seoulit.his.admin.domain.master.patient.dto.PatientResolveResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * [B-2] PatientService.resolveByNamePhone 단위 테스트.
 *
 * 검증 대상: 프론트엔드의 patientId = Date.now() 임시 ID 발명 구조를 대체한
 *           이름+전화 기반 조회/생성 로직.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService 단위 테스트 — B-2 resolve")
class PatientServiceTest {

    @Mock PatientRepository patients;
    @Mock MasterAuditClient audit;

    @InjectMocks
    PatientService patientService;

    @Test
    @DisplayName("이름+전화가 일치하는 기존 환자가 있으면 기존 patientId를 재사용한다 (isNew=false)")
    void resolve_WhenExistingPatient_ShouldReuse() {
        // given
        Patient existing = Patient.builder()
                .patientId(2001L)
                .name("박서준")
                .gender("M")
                .phone("010-1234-5678")
                .active(true)
                .build();
        given(patients.findFirstByNameAndPhoneOrderByPatientIdAsc("박서준", "010-1234-5678"))
                .willReturn(Optional.of(existing));

        PatientResolveRequest req = new PatientResolveRequest(
                "박서준", "M", "010-1234-5678", "950315", "1111111");

        // when
        PatientResolveResponse res = patientService.resolveByNamePhone(req);

        // then — 기존 환자 재사용, 신규 저장 없음
        assertThat(res.patientId()).isEqualTo(2001L);
        assertThat(res.isNew()).isFalse();
        verify(patients, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("일치하는 환자가 없으면 신규 생성하고 DB 채번 ID를 반환한다 (isNew=true)")
    void resolve_WhenNoMatch_ShouldCreateNew() {
        // given
        given(patients.findFirstByNameAndPhoneOrderByPatientIdAsc(anyString(), anyString()))
                .willReturn(Optional.empty());
        Patient saved = Patient.builder()
                .patientId(3000L)   // DB IDENTITY 채번 결과 모사
                .name("김신규")
                .gender("F")
                .phone("010-9999-0000")
                .active(true)
                .build();
        given(patients.save(any(Patient.class))).willReturn(saved);

        PatientResolveRequest req = new PatientResolveRequest(
                "김신규", "F", "010-9999-0000", "000000", "2000000");

        // when
        PatientResolveResponse res = patientService.resolveByNamePhone(req);

        // then — 신규 생성, patientId 미지정 상태로 save 호출(IDENTITY 채번 위임)
        assertThat(res.patientId()).isEqualTo(3000L);
        assertThat(res.isNew()).isTrue();
        verify(patients, times(1)).save(argThat(p -> p.getPatientId() == null));
    }

    @Test
    @DisplayName("동일 이름+전화로 두 번째 resolve 시 첫 번째와 같은 patientId가 반환된다")
    void resolve_SecondCallWithSameNamePhone_ShouldReturnSameId() {
        // given — 첫 호출로 이미 저장된 환자가 DB에 존재하는 상태를 모사
        Patient firstSaved = Patient.builder()
                .patientId(3000L)
                .name("이중복")
                .gender("M")
                .phone("010-1111-2222")
                .active(true)
                .build();
        given(patients.findFirstByNameAndPhoneOrderByPatientIdAsc("이중복", "010-1111-2222"))
                .willReturn(Optional.of(firstSaved));

        PatientResolveRequest req = new PatientResolveRequest(
                "이중복", "M", "010-1111-2222", "880101", "1234567");

        // when — 두 번째 호출
        PatientResolveResponse res = patientService.resolveByNamePhone(req);

        // then — 신규 생성 없이 기존 ID 반환
        assertThat(res.patientId()).isEqualTo(3000L);
        assertThat(res.isNew()).isFalse();
        verify(patients, never()).save(any(Patient.class));
    }
}
