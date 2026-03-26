package kr.co.seoulit.his.support.domain.lab;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    boolean existsByOrderIdAndArchivedFalse(Long orderId);
    Optional<LabResult> findByLabResultIdAndArchivedFalse(Long labResultId);
    Optional<LabResult> findByIdempotencyKey(String idempotencyKey);
    List<LabResult> findByOrderIdAndArchivedFalseOrderByCreatedAtDesc(Long orderId);
}
