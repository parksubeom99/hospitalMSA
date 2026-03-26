package kr.co.seoulit.his.admin.domain.master.patient;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Patient {

    /** 병원 전체에서 공통으로 사용되는 환자 식별자 (다른 서비스와 연동을 위해 직접 지정) */
    @Id
    private Long patientId;

    @Column(nullable = false)
    private String name;

    /** M/F/UNKNOWN */
    @Column(nullable = false)
    private String gender;

    /** 화면용 주민번호 마스킹 (예: 900101-1******) */
    private String rrnMasked;

    private LocalDate birthDate;
    private String phone;

    @Column(nullable = false)
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
