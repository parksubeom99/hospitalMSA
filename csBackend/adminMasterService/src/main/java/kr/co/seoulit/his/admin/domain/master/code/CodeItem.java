package kr.co.seoulit.his.admin.domain.master.code;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CodeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codeItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_set_id", nullable = false)
    private CodeSet codeSet;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int sortOrder = 0;
}
