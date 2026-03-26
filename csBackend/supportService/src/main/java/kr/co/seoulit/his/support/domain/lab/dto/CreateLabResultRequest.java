package kr.co.seoulit.his.support.domain.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLabResultRequest(
        @NotNull Long orderId,
        @NotBlank String resultText,
        @NotBlank String idempotencyKey
) {}
