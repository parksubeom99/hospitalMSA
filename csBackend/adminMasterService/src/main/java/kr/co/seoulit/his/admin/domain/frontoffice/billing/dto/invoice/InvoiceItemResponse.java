package kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice;

public record InvoiceItemResponse(
        Long invoiceItemId,
        String itemCode,
        String itemName,
        Long unitPrice,
        Integer qty,
        Long lineTotal
) {}
