package kr.co.seoulit.his.support.domain.radiology;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RadiologyReportRepository extends JpaRepository<RadiologyReport, Long> {
    boolean existsByOrderIdAndArchivedFalse(Long orderId);
    Optional<RadiologyReport> findByRadiologyReportIdAndArchivedFalse(Long radiologyReportId);
    Optional<RadiologyReport> findByIdempotencyKey(String idempotencyKey);
    List<RadiologyReport> findByOrderIdAndArchivedFalseOrderByCreatedAtDesc(Long orderId);
}
