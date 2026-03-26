package kr.co.seoulit.his.admin.domain.frontoffice.billing;

import jakarta.validation.Valid;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice.*;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.payment.PaymentCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.payment.PaymentResponse;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.receipt.ReceiptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService service;

    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceCreateRequest req) {
        return ResponseEntity.ok(service.createInvoice(req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(@RequestParam(required = false) Long visitId) {
        return ResponseEntity.ok(service.listInvoices(visitId));
    }

    // ✅ 청구 정정(이력)
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/invoices/{invoiceId}/adjust")
    public ResponseEntity<InvoiceResponse> adjust(@PathVariable Long invoiceId, @Valid @RequestBody InvoiceAdjustRequest req) {
        return ResponseEntity.ok(service.adjustInvoice(invoiceId, req));
    }

    // ✅ 청구 취소(이력)
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/invoices/{invoiceId}/cancel")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable Long invoiceId, @RequestBody(required = false) InvoiceCancelRequest req) {
        return ResponseEntity.ok(service.cancelInvoice(invoiceId, req));
    }

    // ✅ 영수증/이력 조회
    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @GetMapping("/invoices/{invoiceId}/history")
    public ResponseEntity<InvoiceHistoryResponse> history(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(service.history(invoiceId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentCreateRequest req) {
        return ResponseEntity.ok(service.pay(req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SYS')")
    @GetMapping("/receipts/by-payment/{paymentId}")
    public ResponseEntity<ReceiptResponse> receiptByPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(service.getReceiptByPayment(paymentId));
    }
}
