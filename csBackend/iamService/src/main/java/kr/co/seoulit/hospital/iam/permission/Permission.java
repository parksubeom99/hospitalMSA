package kr.co.seoulit.hospital.iam.permission;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="permission")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Permission {

    @Id
    @Column(length=40)
    private String permCode; // VISIT_CREATE, ORDER_START ...

    @Column(nullable=false)
    private String permName;

    private String description;
}
