package kr.co.seoulit.his.admin.domain.frontoffice.visit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByStatus(String status);
}
