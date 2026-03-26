package kr.co.seoulit.his.support.domain.worklist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorklistTaskRepository extends JpaRepository<WorklistTask, Long> {

    Optional<WorklistTask> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<WorklistTask> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);


    List<WorklistTask> findByCategoryAndStatusAndPrimaryItemCodeOrderByCreatedAtDesc(String category, String status, String primaryItemCode);
}
