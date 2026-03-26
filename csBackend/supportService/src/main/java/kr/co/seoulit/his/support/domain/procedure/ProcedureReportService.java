package kr.co.seoulit.his.support.domain.procedure;

import kr.co.seoulit.his.support.domain.procedure.dto.CreateProcedureReportRequest;
import kr.co.seoulit.his.support.domain.procedure.dto.ProcedureReportResponse;
import kr.co.seoulit.his.support.domain.procedure.dto.UpdateProcedureReportRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcedureReportService {

    private final ProcedureReportRepository repo;

    @Transactional
    public ProcedureReportResponse create(CreateProcedureReportRequest req) {
        // idempotencyKey는 unique라서 중복이면 DB에서 막힙니다.
        ProcedureReport saved = repo.save(ProcedureReport.builder()
                .orderId(req.orderId())
                .reportText(req.reportText())
                .status("DONE")
                .idempotencyKey(req.idempotencyKey())
                .createdAt(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProcedureReportResponse> list(Long orderId) {
        List<ProcedureReport> rows = (orderId == null)
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findByOrderIdOrderByCreatedAtDesc(orderId);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProcedureReportResponse get(Long id) {
        ProcedureReport e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Procedure report not found. id=" + id));
        return toResponse(e);
    }

    @Transactional
    public ProcedureReportResponse update(Long id, UpdateProcedureReportRequest req) {
        ProcedureReport e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Procedure report not found. id=" + id));
        e.setReportText(req.reportText());
        // status는 최소 수정으로 유지(DONE). 필요 시 확장 가능
        ProcedureReport saved = repo.save(e);
        return toResponse(saved);
    }

    private ProcedureReportResponse toResponse(ProcedureReport e) {
        return new ProcedureReportResponse(
                e.getProcedureReportId(),
                e.getOrderId(),
                e.getReportText(),
                e.getStatus(),
                e.getIdempotencyKey(),
                e.getCreatedAt()
        );
    }
}
