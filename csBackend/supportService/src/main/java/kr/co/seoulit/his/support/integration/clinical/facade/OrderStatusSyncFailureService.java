package kr.co.seoulit.his.support.integration.clinical.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusSyncFailureService {

    private final OrderStatusSyncFailureRepository repo;

    @Transactional
    public void recordFailure(Long orderId, String targetStatus, String endpointUrl, String errorMessage, String traceId) {
        LocalDateTime now = LocalDateTime.now();
        repo.save(OrderStatusSyncFailure.builder()
                .orderId(orderId)
                .targetStatus(targetStatus)
                .endpointUrl(endpointUrl)
                .errorMessage(trim(errorMessage, 1900))
                .traceId(traceId)
                .syncStatus("PENDING")
                .retryCount(0)
                .nextRetryAt(now.plusSeconds(15))
                .lastTriedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Transactional
    public void markSuccess(OrderStatusSyncFailure f) {
        LocalDateTime now = LocalDateTime.now();
        f.setSyncStatus("SUCCESS");
        f.setSucceededAt(now);
        f.setUpdatedAt(now);
        repo.save(f);
    }

    @Transactional
    public void reschedule(OrderStatusSyncFailure f, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        int nextCount = (f.getRetryCount() == null ? 0 : f.getRetryCount()) + 1;
        f.setRetryCount(nextCount);
        f.setLastTriedAt(now);
        f.setUpdatedAt(now);
        f.setErrorMessage(trim(errorMessage, 1900));

        if (nextCount >= 10) {
            f.setSyncStatus("GAVE_UP");
            f.setNextRetryAt(now.plusDays(3650));
        } else {
            long delaySec = Math.min(300, (long) Math.pow(2, Math.min(6, nextCount)) * 5L);
            f.setSyncStatus("PENDING");
            f.setNextRetryAt(now.plusSeconds(delaySec));
        }
        repo.save(f);
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
