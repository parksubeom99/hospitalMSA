package kr.co.seoulit.hospital.iam.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="user_account")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable=false, unique=true)
    private String loginId;

    @Column(nullable=false)
    private String passwordHash;

    // 직원(Staff) 식별자: 기존 employee_id 재사용
    @Column(nullable=false)
    private String staffId;

    @Column(nullable=false)
    private boolean active;
}
