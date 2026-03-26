package kr.co.seoulit.his.admin.messaging.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/outbox")
public class OutboxController {

    private final OutboxService outbox;

    @PreAuthorize("hasRole('SYS')")
    @GetMapping
    public ResponseEntity<List<OutboxEvent>> list(@RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(outbox.listNew(limit));
    }

    @PreAuthorize("hasRole('SYS')")
    @PostMapping("/{id}/mark-published")
    public ResponseEntity<Void> markPublished(@PathVariable Long id) {
        outbox.markPublished(id);
        return ResponseEntity.ok().build();
    }
}
