package kr.co.seoulit.his.admin.domain.master.code;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "code_set")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CodeSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codeSetId;

    @Column(nullable = false, unique = true, length = 50)
    private String codeSetKey; // 예: DEPARTMENT, LAB_TEST, DRUG

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "codeSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CodeItem> items = new ArrayList<>();
}
