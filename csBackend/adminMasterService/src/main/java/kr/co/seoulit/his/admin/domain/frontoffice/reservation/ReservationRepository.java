package kr.co.seoulit.his.admin.domain.frontoffice.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByStatus(String status);
    List<Reservation> findByScheduledAtBetween(LocalDateTime from, LocalDateTime to);
}
