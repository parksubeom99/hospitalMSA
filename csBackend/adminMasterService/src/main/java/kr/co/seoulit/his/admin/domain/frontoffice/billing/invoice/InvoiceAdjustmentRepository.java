package kr.co.seoulit.his.admin.domain.frontoffice.billing.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceAdjustmentRepository extends JpaRepository<InvoiceAdjustment, Long> {
    List<InvoiceAdjustment> findByInvoiceIdOrderByAdjustmentIdAsc(Long invoiceId);
}
