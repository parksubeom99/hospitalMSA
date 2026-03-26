package kr.co.seoulit.hospital.iam.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="user_role")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long userId;

    @Column(nullable=false, length=20)
    private String roleCode;
}
