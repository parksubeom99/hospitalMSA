package kr.co.seoulit.his.clinical.domain.order;

import jakarta.validation.Valid;
import kr.co.seoulit.his.clinical.domain.order.dto.CreateOrderRequest;
import kr.co.seoulit.his.clinical.domain.order.dto.OrderDeleteRequest;
import kr.co.seoulit.his.clinical.domain.order.dto.OrderItemResponse;
import kr.co.seoulit.his.clinical.domain.order.dto.OrderResponse;
import kr.co.seoulit.his.clinical.domain.order.dto.UpdateOrderItemsRequest;
import kr.co.seoulit.his.clinical.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    // ✅ Support(진료지원)에서 Worklist 조회를 할 때, LAB/RAD/PHARM/PROC 토큰이 그대로 전달됩니다.
    //    따라서 조회 권한을 LAB/RAD/PHARM/PROC까지 열어 정합을 맞춥니다.
    @PreAuthorize("hasAnyRole('DOC','NUR','LAB','RAD','PHARM','PROC','SYS')")
    @GetMapping
    public ResponseEntity<List<OrderResponse>> list(
            @RequestParam(required=false) String status,
            @RequestParam(required=false) String category
    ) {
        return ResponseEntity.ok(service.list(status, category));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PutMapping("/{id}/items")
    public ResponseEntity<OrderResponse> updateItems(@PathVariable Long id, @Valid @RequestBody UpdateOrderItemsRequest req) {
        return ResponseEntity.ok(service.updateItems(id, req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    // ✅ Support 결과등록(LAB/RAD/PHARM) 후 DONE 처리가 Support -> Clinical 호출로 들어옵니다.
    //    LAB/RAD/PHARM 권한을 열어줘야 결과등록 성공 후 상태가 실제로 DONE으로 반영됩니다.
    // v5: PROC(시술/내시경) 지원
    @PreAuthorize("hasAnyRole('DOC','NUR','LAB','RAD','PHARM','PROC','SYS')")
    @PostMapping("/{id}/done")
    public ResponseEntity<OrderResponse> done(@PathVariable Long id) {
        return ResponseEntity.ok(service.markDone(id));
    }

    // 결과 입력 완료(Resulted) - 주로 Support(LAB/RAD/PHARM/PROC) 결과입력 이후 상태 전환
    @PreAuthorize("hasAnyRole('DOC','NUR','LAB','RAD','PHARM','PROC','SYS')")
    @PostMapping("/{id}/resulted")
    public ResponseEntity<OrderResponse> resulted(@PathVariable Long id) {
        return ResponseEntity.ok(service.markResulted(id));
    }

    // 의사 확인(Reviewed) - 결과 확인은 DOC 중심
    @PreAuthorize("hasAnyRole('DOC','SYS')")
    @PostMapping("/{id}/reviewed")
    public ResponseEntity<OrderResponse> reviewed(@PathVariable Long id) {
        return ResponseEntity.ok(service.markReviewed(id));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','LAB','RAD','PHARM','PROC','SYS')")
    @PostMapping("/{id}/in-progress")
    public ResponseEntity<OrderResponse> inProgress(@PathVariable Long id) {
        return ResponseEntity.ok(service.markInProgress(id));
    }

    // =========================
    // [ADDED] Soft Delete API (A안)
    // POST /orders/{id}/delete
    // =========================
    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @PostMapping("/{id}/delete")
    public ResponseEntity<OrderResponse> delete(@PathVariable Long id, @Valid @RequestBody(required = false) OrderDeleteRequest req) {
        return ResponseEntity.ok(service.delete(id, req));
    }

    @PreAuthorize("hasAnyRole('DOC','NUR','SYS')")
    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItemResponse>> items(@PathVariable Long id) {
        return ResponseEntity.ok(service.listItems(id));
    }
}
