package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InvoiceGenerateRequest(
        @NotNull Long visitId,
        @NotEmpty List<@Valid InvoiceItemCreateRequest> items
) {}
