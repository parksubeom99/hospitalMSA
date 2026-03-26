package kr.co.seoulit.his.admin.domain.master.staff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long>, JpaSpecificationExecutor<StaffProfile> {
    Optional<StaffProfile> findByLoginId(String loginId);
}
