package kr.co.seoulit.his.admin.domain.master.patientalert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientAlertRepository extends JpaRepository<PatientAlert, Long> {
    List<PatientAlert> findByPatientIdInAndActive(List<Long> patientIds, Boolean active);
}
