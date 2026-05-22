package kr.co.seoulit.his.admin.domain.master.patient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    /** [B-2] 이름+전화가 일치하는 환자 조회. 중복 시 가장 먼저 등록된 1건. */
    Optional<Patient> findFirstByNameAndPhoneOrderByPatientIdAsc(String name, String phone);
}
