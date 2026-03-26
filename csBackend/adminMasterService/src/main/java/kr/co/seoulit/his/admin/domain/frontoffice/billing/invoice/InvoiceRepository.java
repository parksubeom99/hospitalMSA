package kr.co.seoulit.his.admin.domain.frontoffice.billing.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByVisitId(Long visitId);
    boolean existsByVisitIdAndStatus(Long visitId, String status);
}
