package kr.co.seoulit.his.support.domain.pharmacy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispenseRepository extends JpaRepository<Dispense, Long> {
    boolean existsByOrderIdAndArchivedFalse(Long orderId);
    Optional<Dispense> findByDispenseIdAndArchivedFalse(Long dispenseId);
    Optional<Dispense> findByIdempotencyKey(String idempotencyKey);
}
