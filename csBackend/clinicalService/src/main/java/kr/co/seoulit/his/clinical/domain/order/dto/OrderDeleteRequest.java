package kr.co.seoulit.his.clinical.domain.order.dto;

import jakarta.validation.constraints.Size;

public record OrderDeleteRequest(
        @Size(max = 255) String reason
) {}
