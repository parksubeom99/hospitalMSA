package kr.co.seoulit.his.clinical.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderHeader, Long> {

    // =========================
    // [ADDED] Soft Delete filters
    // =========================
    Optional<OrderHeader> findByOrderIdAndDeletedFalse(Long orderId);
    List<OrderHeader> findAllByDeletedFalse();

    List<OrderHeader> findByVisitIdAndDeletedFalse(Long visitId);

    List<OrderHeader> findByStatusAndDeletedFalse(String status);
    List<OrderHeader> findByStatusAndCategoryAndDeletedFalse(String status, String category);

    Optional<OrderHeader> findByIdempotencyKeyAndDeletedFalse(String idempotencyKey);
}
