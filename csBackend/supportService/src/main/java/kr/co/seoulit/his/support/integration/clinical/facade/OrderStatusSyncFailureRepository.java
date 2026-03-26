package kr.co.seoulit.his.support.integration.clinical.facade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderStatusSyncFailureRepository extends JpaRepository<OrderStatusSyncFailure, Long> {

    List<OrderStatusSyncFailure> findTop50BySyncStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(String syncStatus, LocalDateTime dueTime);
}
