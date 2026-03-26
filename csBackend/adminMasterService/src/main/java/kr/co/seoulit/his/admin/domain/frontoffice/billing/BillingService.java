package kr.co.seoulit.his.admin.domain.frontoffice.billing;

import kr.co.seoulit.his.admin.audit.AuditClient;
import kr.co.seoulit.his.admin.global.exception.BusinessException;
import kr.co.seoulit.his.admin.global.exception.ErrorCode;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.invoice.*;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.payment.PaymentCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.payment.PaymentResponse;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.dto.receipt.ReceiptResponse;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.invoice.*;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.payment.Payment;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.payment.PaymentRepository;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.receipt.Receipt;
import kr.co.seoulit.his.admin.domain.frontoffice.billing.receipt.ReceiptRepository;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.VisitService;
import kr.co.seoulit.his.admin.domain.frontoffice.visit.dto.VisitStatusUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final InvoiceRepository invoices;
    // [FIX] InvoiceItemRepository 주입(InvoiceResponse.items 채우기 + TOTAL 라인 아이템 관리)
    private final InvoiceItemRepository invoiceItems;
    private final PaymentRepository payments;
    private final ReceiptRepository receipts;
    private final InvoiceAdjustmentRepository adjustments;
    private final VisitService visits;
    private final AuditClient audit;

    /**
     * ✅ 청구 생성
     * - InvoiceCreateRequest(amount) -> Invoice.totalAmount(Long)으로 정합화
     * - InvoiceItem 1줄(TOTAL) 자동 생성
     */
    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest req) {
        // [FIX] Invoice 엔티티 필드명(totalAmount)에 맞춰 저장
        long total = req.amount() == null ? 0L : req.amount().longValue();

        Invoice invoice = Invoice.builder()
                .visitId(req.visitId())
                .totalAmount(total) // [FIX]
                .status("ISSUED")
                .createdAt(LocalDateTime.now())
                .build();
        Invoice saved = invoices.save(invoice);

        // [FIX] 단일 라인 아이템(TOTAL) 생성
        upsertTotalItem(saved.getInvoiceId(), total);

        audit.write("INVOICE_CREATED", "INVOICE", String.valueOf(saved.getInvoiceId()), null,
                Map.of("totalAmount", String.valueOf(saved.getTotalAmount()), "status", saved.getStatus())); // [FIX]

        return toInvoiceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(Long visitId) {
        List<Invoice> list = (visitId == null) ? invoices.findAll() : invoices.findByVisitId(visitId);
        return list.stream().map(this::toInvoiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByPayment(Long paymentId) {
        Receipt r = receipts.findByPaymentId(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Receipt not found. paymentId=" + paymentId));

        // [FIX] Receipt 엔티티/DTO 정합화(issuedAt/content -> receiptNo/createdAt)
        return new ReceiptResponse(r.getReceiptId(), r.getPaymentId(), r.getReceiptNo(), r.getCreatedAt());
    }

    /**
     * ✅ 청구 정정(append-only 이력 + invoice.totalAmount 갱신)
     * - PAID/CANCELED는 정정 불가
     */
    @Transactional
    public InvoiceResponse adjustInvoice(Long invoiceId, InvoiceAdjustRequest req) {
        Invoice inv = invoices.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Invoice not found. invoiceId=" + invoiceId));

        String st = norm(inv.getStatus());
        if (!"ISSUED".equals(st)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Only ISSUED invoice can be adjusted. status=" + st);
        }

        int oldAmount = inv.getTotalAmount() == null ? 0 : safeToInt(inv.getTotalAmount()); // [FIX]
        int newAmount = req.newAmount();
        if (newAmount < 0) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "newAmount must be >= 0");

        inv.setTotalAmount((long) newAmount); // [FIX]
        inv.setUpdatedAt(LocalDateTime.now());
        Invoice saved = invoices.save(inv);

        // [FIX] TOTAL 라인 아이템도 동기화(있는 경우 update, 없으면 insert)
        upsertTotalItem(saved.getInvoiceId(), saved.getTotalAmount());

        adjustments.save(InvoiceAdjustment.builder()
                .invoiceId(saved.getInvoiceId())
                .type("ADJUST")
                .oldAmount(oldAmount)
                .newAmount(newAmount)
                .reason(req.reason())
                .createdAt(LocalDateTime.now())
                .build());

        audit.write("INVOICE_ADJUSTED", "INVOICE", String.valueOf(saved.getInvoiceId()), null,
                Map.of("oldAmount", String.valueOf(oldAmount), "newAmount", String.valueOf(newAmount)));

        return toInvoiceResponse(saved);
    }

    /**
     * ✅ 청구 취소(append-only 이력 + invoice.status=CANCELED)
     * - PAID는 취소 불가(환불 프로세스는 별도)
     */
    @Transactional
    public InvoiceResponse cancelInvoice(Long invoiceId, InvoiceCancelRequest req) {
        Invoice inv = invoices.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Invoice not found. invoiceId=" + invoiceId));

        String st = norm(inv.getStatus());
        if ("PAID".equals(st)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Cannot cancel PAID invoice. (refund not implemented)");
        }
        if ("CANCELED".equals(st)) return toInvoiceResponse(inv);

        int oldAmount = inv.getTotalAmount() == null ? 0 : safeToInt(inv.getTotalAmount()); // [FIX]
        inv.setStatus("CANCELED");
        inv.setUpdatedAt(LocalDateTime.now());
        Invoice saved = invoices.save(inv);

        adjustments.save(InvoiceAdjustment.builder()
                .invoiceId(saved.getInvoiceId())
                .type("CANCEL")
                .oldAmount(oldAmount)
                .newAmount(oldAmount)
                .reason(req == null ? null : req.reason())
                .createdAt(LocalDateTime.now())
                .build());

        audit.write("INVOICE_CANCELED", "INVOICE", String.valueOf(saved.getInvoiceId()), null,
                Map.of("reason", req == null ? "" : (req.reason() == null ? "" : req.reason())));

        return toInvoiceResponse(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceHistoryResponse history(Long invoiceId) {
        Invoice inv = invoices.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Invoice not found. invoiceId=" + invoiceId));

        List<InvoiceAdjustmentResponse> list = adjustments.findByInvoiceIdOrderByAdjustmentIdAsc(invoiceId).stream()
                .map(a -> new InvoiceAdjustmentResponse(
                        a.getAdjustmentId(),
                        a.getType(),
                        a.getOldAmount(),
                        a.getNewAmount(),
                        a.getReason(),
                        a.getCreatedAt()
                ))
                .toList();

        return new InvoiceHistoryResponse(toInvoiceResponse(inv), list);
    }

    /**
     * ✅ 결제(PAY)
     * - idempotencyKey가 있으면 중복 결제 방지
     * - 결제 성공 시 invoice.status=PAID, receipt 생성
     * - 방문 상태를 COMPLETED로 업데이트
     */
    @Transactional
    public PaymentResponse pay(PaymentCreateRequest req) {
        // [FIX] idempotencyKey 중복 방지
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            Payment existed = payments.findByIdempotencyKey(req.idempotencyKey()).orElse(null);
            if (existed != null) {
                return toPaymentResponse(existed);
            }
        }

        Invoice inv = invoices.findById(req.invoiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Invoice not found. invoiceId=" + req.invoiceId()));

        if (!"ISSUED".equals(norm(inv.getStatus()))) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Invoice is not payable. status=" + inv.getStatus());
        }

        // [FIX] 결제 금액 검증(Invoice.totalAmount와 동일해야 함)
        long invoiceTotal = inv.getTotalAmount() == null ? 0L : inv.getTotalAmount();
        if (req.amount() != null && req.amount() != invoiceTotal) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Payment amount mismatch. invoiceTotal=" + invoiceTotal + ", paid=" + req.amount());
        }

        Payment p = Payment.builder()
                .invoiceId(inv.getInvoiceId())
                .method(req.method())
                .amount(req.amount())
                .status("PAID")
                .idempotencyKey(blankToNull(req.idempotencyKey()))
                .paidAt(LocalDateTime.now())
                .build();
        Payment pay = payments.save(p);

        inv.setStatus("PAID");
        inv.setUpdatedAt(LocalDateTime.now());
        invoices.save(inv);

        // [FIX] Receipt 엔티티 필드 정합화(receiptNo, createdAt)
        Receipt receipt = Receipt.builder()
                .paymentId(pay.getPaymentId())
                .createdAt(LocalDateTime.now())
                .build();
        Receipt savedReceipt = receipts.save(receipt);

        // receiptNo 생성: RCT-YYYYMMDD-00001 형태
        String receiptNo = buildReceiptNo(savedReceipt.getReceiptId());
        savedReceipt.setReceiptNo(receiptNo);
        receipts.save(savedReceipt);

        // ✅ 수납 완료 시 방문 완료(COMPLETED)
        // [FIX] Visit이 admin_visit에 없는 경우(로컬 데모 visitId 등) 결제 자체는 성공 처리
        try {
            visits.updateStatus(inv.getVisitId(), new VisitStatusUpdateRequest("COMPLETED"));
        } catch (Exception e) {
            log.warn("[BillingService] Visit status update skipped. visitId={}, reason={}", inv.getVisitId(), e.getMessage());
        }

        audit.write("PAYMENT_COMPLETED", "PAYMENT", String.valueOf(pay.getPaymentId()), null,
                Map.of(
                        "invoiceId", String.valueOf(inv.getInvoiceId()),
                        "method", pay.getMethod(),
                        "amount", String.valueOf(pay.getAmount())
                ));

        return toPaymentResponse(pay);
    }

    // =========================
    // Mapper helpers
    // =========================

    private InvoiceResponse toInvoiceResponse(Invoice i) {
        // [FIX] InvoiceResponse 시그니처 정합화 + items 채우기
        List<InvoiceItemResponse> items = invoiceItems.findByInvoiceId(i.getInvoiceId()).stream()
                .map(this::toInvoiceItemResponse)
                .toList();

        return new InvoiceResponse(
                i.getInvoiceId(),
                i.getVisitId(),
                i.getStatus(),
                i.getTotalAmount(),
                i.getCreatedAt(),
                i.getUpdatedAt(),
                items
        );
    }

    private InvoiceItemResponse toInvoiceItemResponse(InvoiceItem it) {
        return new InvoiceItemResponse(
                it.getInvoiceItemId(),
                it.getItemCode(),
                it.getItemName(),
                it.getUnitPrice(),
                it.getQty(),
                it.getLineTotal()
        );
    }

    private PaymentResponse toPaymentResponse(Payment p) {
        // [FIX] PaymentResponse 시그니처 정합화
        return new PaymentResponse(
                p.getPaymentId(),
                p.getInvoiceId(),
                p.getMethod(),
                p.getAmount(),
                p.getStatus(),
                p.getIdempotencyKey(),
                p.getPaidAt()
        );
    }

    // =========================
    // Internal utilities
    // =========================

    private void upsertTotalItem(Long invoiceId, long totalAmount) {
        // [FIX] 기존 TOTAL 아이템이 있으면 업데이트, 없으면 생성
        List<InvoiceItem> list = invoiceItems.findByInvoiceId(invoiceId);
        InvoiceItem total = list.stream()
                .filter(it -> "TOTAL".equalsIgnoreCase(it.getItemCode()))
                .findFirst()
                .orElse(null);

        if (total == null) {
            total = InvoiceItem.builder()
                    .invoiceId(invoiceId)
                    .itemCode("TOTAL")
                    .itemName("Total Amount")
                    .unitPrice(totalAmount)
                    .qty(1)
                    .lineTotal(totalAmount)
                    .build();
        } else {
            total.setUnitPrice(totalAmount);
            total.setQty(1);
            total.setLineTotal(totalAmount);
        }
        invoiceItems.save(total);
    }

    private static int safeToInt(Long v) {
        if (v == null) return 0;
        if (v > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (v < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return v.intValue();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String buildReceiptNo(Long receiptId) {
        String ymd = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "RCT-" + ymd + "-" + String.format("%05d", receiptId == null ? 0 : receiptId);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}