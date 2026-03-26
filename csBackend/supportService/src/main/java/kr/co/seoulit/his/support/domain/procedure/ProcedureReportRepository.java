package kr.co.seoulit.his.support.domain.procedure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcedureReportRepository extends JpaRepository<ProcedureReport, Long> {

    List<ProcedureReport> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<ProcedureReport> findAllByOrderByCreatedAtDesc();
}
