package kr.co.seoulit.his.admin.domain.master.patient.dto;

/**
 * [B-2] 환자 조회/생성 결과.
 *
 * isNew=false → 동일 이름+전화의 기존 환자를 재사용한 경우.
 * isNew=true  → 신규 환자를 생성하고 DB IDENTITY로 채번한 경우.
 */
public record PatientResolveResponse(
        Long patientId,
        String name,
        boolean isNew
) {}
