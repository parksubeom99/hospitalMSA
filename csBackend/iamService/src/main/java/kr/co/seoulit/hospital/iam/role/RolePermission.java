package kr.co.seoulit.hospital.iam.role;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="role_permission",
        uniqueConstraints = @UniqueConstraint(name="uk_role_perm", columnNames = {"roleCode","permCode"}),
        indexes = {
                @Index(name="idx_role_perm_role", columnList="roleCode"),
                @Index(name="idx_role_perm_perm", columnList="permCode")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=20)
    private String roleCode;

    @Column(nullable=false, length=40)
    private String permCode;
}
