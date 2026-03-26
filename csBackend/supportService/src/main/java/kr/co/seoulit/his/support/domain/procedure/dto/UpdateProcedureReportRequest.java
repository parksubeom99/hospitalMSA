package kr.co.seoulit.his.support.domain.procedure.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * PROC(시술) 결과 수정 요청
 * - 최소 수정: reportText만 수정
 * - status는 기본적으로 DONE 유지(필요 시 확장)
 */
public record UpdateProcedureReportRequest(
        @NotBlank String reportText
) {}
