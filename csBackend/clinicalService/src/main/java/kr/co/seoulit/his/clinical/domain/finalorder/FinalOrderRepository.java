package kr.co.seoulit.his.clinical.domain.finalorder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinalOrderRepository extends JpaRepository<FinalOrder, Long> {
    Optional<FinalOrder> findByIdempotencyKey(String idempotencyKey);

    // Phase 2: visitId 기준 최종오더 집계 (BILLING_REQUESTED 발행 조건 체크)
    List<FinalOrder> findByVisitId(Long visitId);
}
