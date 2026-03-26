package kr.co.seoulit.his.admin.domain.frontoffice.reservation;

import jakarta.validation.Valid;
import kr.co.seoulit.his.admin.domain.frontoffice.reservation.dto.ReservationCreateRequest;
import kr.co.seoulit.his.admin.domain.frontoffice.reservation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(service.list(status, date));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.cancel(id, reason));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','ADMIN','SYS')")
    @PostMapping("/{id}/check-in")
    public ResponseEntity<ReservationResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(service.checkIn(id));
    }
}
