package kr.co.seoulit.his.support.integration.clinical.facade;

import kr.co.seoulit.his.support.integration.clinical.client.OrderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusSyncRetryScheduler {

    private final OrderStatusSyncFailureRepository repo;
    private final OrderStatusSyncFailureService failureService;
    private final OrderClient orderClient;

    @Scheduled(fixedDelayString = "${support.order-sync.retry.delay-ms:15000}")
    @Transactional
    public void retryPending() {
        List<OrderStatusSyncFailure> due = repo.findTop50BySyncStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc("PENDING", LocalDateTime.now());
        for (OrderStatusSyncFailure f : due) {
            try {
                boolean ok = orderClient.retrySyncStatus(f.getOrderId(), f.getTargetStatus(), f.getTraceId());
                if (ok) {
                    failureService.markSuccess(f);
                } else {
                    failureService.reschedule(f, "retry returned false");
                }
            } catch (Exception e) {
                log.warn("Order status retry failed id={}, orderId={}, targetStatus={}", f.getId(), f.getOrderId(), f.getTargetStatus(), e);
                failureService.reschedule(f, e.getMessage());
            }
        }
    }
}
