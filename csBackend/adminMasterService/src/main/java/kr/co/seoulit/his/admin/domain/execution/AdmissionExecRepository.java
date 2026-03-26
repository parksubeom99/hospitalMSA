package kr.co.seoulit.his.admin.domain.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface AdmissionExecRepository extends JpaRepository<AdmissionExec, Long> {
    Optional<AdmissionExec> findByIdempotencyKey(String idempotencyKey);

    List<AdmissionExec> findAllByFinalOrderId(Long finalOrderId);
}
