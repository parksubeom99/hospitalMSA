package kr.co.seoulit.his.admin.domain.frontoffice.queue.dto;

import jakarta.validation.constraints.NotNull;

public record QueueCreateRequest(
        @NotNull Long visitId,
        String category
) {}
