package kr.co.seoulit.his.admin.domain.master.staff;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_profile")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long staffProfileId;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId; // IAM user subject와 연결

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String jobType; // DOCTOR/NURSE/ADMIN/LAB...

    @Column
    private Long departmentId;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private boolean active = true;
}
