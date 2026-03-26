package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * ✅ 청구 생성 요청(Invoice Create Request)
 * - BillingController.createInvoice() / BillingService.createInvoice()에서 사용
 */
public record InvoiceCreateRequest(
        @NotNull(message = "visitId는 필수입니다.")
        Long visitId,

        @NotNull(message = "amount는 필수입니다.")
        @Min(value = 0, message = "amount는 0 이상이어야 합니다.")
        Integer amount
) {
}