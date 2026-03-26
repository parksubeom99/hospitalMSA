package kr.co.seoulit.his.admin.domain.master.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorScheduleTemplateRepository extends JpaRepository<DoctorScheduleTemplate, Long> {
    List<DoctorScheduleTemplate> findByStaffProfileIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long staffProfileId);
    Page<DoctorScheduleTemplate> findByStaffProfileId(Long staffProfileId, Pageable pageable);
}
