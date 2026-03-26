package kr.co.seoulit.his.support.domain.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedExecRepository extends JpaRepository<MedExec, Long> {
    Optional<MedExec> findByIdempotencyKey(String idempotencyKey);
    List<MedExec> findAllByFinalOrderId(Long finalOrderId);
}
