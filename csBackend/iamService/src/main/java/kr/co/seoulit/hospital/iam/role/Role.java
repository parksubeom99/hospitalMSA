package kr.co.seoulit.hospital.iam.role;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="role")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @Column(length=20)
    private String roleCode; // DOC/NUR/LAB/RAD/PHARM/ADMIN/SYS

    @Column(nullable=false)
    private String roleName;
}
