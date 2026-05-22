package kr.co.seoulit.his.admin.domain.master.patient.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [B-2] 이름+전화로 환자를 조회하고, 없으면 신규 생성하기 위한 요청 DTO.
 *
 * 기존에는 프론트엔드가 patientId = Date.now() 로 임시 ID를 발명해 INSERT 했다.
 * 본 DTO는 patientId를 받지 않으며, 신규 생성 시 DB IDENTITY가 채번한다.
 */
public record PatientResolveRequest(
        @NotBlank String name,
        @NotBlank String gender,
        String phone,
        String rrnFront,
        String rrnBack
) {}
