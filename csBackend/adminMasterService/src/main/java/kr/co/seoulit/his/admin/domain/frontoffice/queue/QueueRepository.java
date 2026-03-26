package kr.co.seoulit.his.admin.domain.frontoffice.queue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QueueRepository extends JpaRepository<QueueTicket, Long> {
    List<QueueTicket> findByStatus(String status);
    List<QueueTicket> findByCategory(String category);
    List<QueueTicket> findByCategoryAndStatus(String category, String status);

    Optional<QueueTicket> findTopByVisitIdAndCategoryOrderByTicketIdDesc(Long visitId, String category);
}
