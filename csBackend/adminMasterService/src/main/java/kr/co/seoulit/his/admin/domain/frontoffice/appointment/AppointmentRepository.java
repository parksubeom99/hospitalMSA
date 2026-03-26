package kr.co.seoulit.his.admin.domain.frontoffice.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByScheduledAtBetween(LocalDateTime fromInclusive, LocalDateTime toExclusive);
    List<Appointment> findByStatusAndScheduledAtBetween(String status, LocalDateTime fromInclusive, LocalDateTime toExclusive);
}
