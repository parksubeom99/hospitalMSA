package kr.co.seoulit.his.admin.domain.frontoffice.queue;

import jakarta.validation.Valid;
import kr.co.seoulit.his.admin.domain.frontoffice.queue.dto.QueueCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.queue.dto.QueueResponse;
import kr.co.seoulit.his.admin.domain.frontoffice.queue.dto.QueueSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService service;
    private final QueueMetricsService metrics;

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping
    public ResponseEntity<QueueResponse> create(@Valid @RequestBody QueueCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @GetMapping
    public ResponseEntity<List<QueueResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(service.list(status, category));
    }

    // 비CRUD(워크플로우): 호출
    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping("/{ticketId}/call")
    public ResponseEntity<QueueResponse> call(@PathVariable Long ticketId) {
        return ResponseEntity.ok(service.call(ticketId));
    }

    // 비CRUD(워크플로우): 완료
    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping("/{ticketId}/done")
    public ResponseEntity<QueueResponse> done(@PathVariable Long ticketId) {
        return ResponseEntity.ok(service.done(ticketId));
    }

    // 대기현황(현재 대기 n명/예상시간)
    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @GetMapping("/summary")
    public ResponseEntity<QueueSummaryResponse> summary(@RequestParam String category) {
        QueueMetricsService.Metrics m = metrics.calc(category);
        return ResponseEntity.ok(new QueueSummaryResponse(
                m.category(),
                m.waitingCount(),
                m.sampleSize(),
                m.avgServiceMinutes(),
                m.estimatedMinutesForNew(),
                m.calculatedAt()
        ));
    }
}
