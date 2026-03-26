package kr.co.seoulit.his.support.domain.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface EndoscopyReportRepository extends JpaRepository<EndoscopyReport, Long> {
    Optional<EndoscopyReport> findByIdempotencyKey(String idempotencyKey);

    List<EndoscopyReport> findAllByFinalOrderId(Long finalOrderId);
}
