package kr.co.seoulit.his.admin.domain.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface SurgeryExecRepository extends JpaRepository<SurgeryExec, Long> {
    Optional<SurgeryExec> findByIdempotencyKey(String idempotencyKey);

    List<SurgeryExec> findAllByFinalOrderId(Long finalOrderId);
}
