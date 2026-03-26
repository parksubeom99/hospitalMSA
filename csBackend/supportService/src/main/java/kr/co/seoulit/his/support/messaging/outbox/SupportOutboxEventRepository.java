package kr.co.seoulit.his.support.messaging.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupportOutboxEventRepository extends JpaRepository<SupportOutboxEvent, Long> {

    boolean existsByDedupKey(String dedupKey);

    @Query("select e from SupportOutboxEvent e where e.status = :status order by e.id asc")
    List<SupportOutboxEvent> findByStatusOrderByIdAsc(OutboxStatus status, Pageable pageable);
}
