package kr.co.seoulit.his.clinical.domain.emr.soap.history;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit_soap_history")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SoapNoteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(nullable = false)
    private Long visitId;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(length = 4000)
    private String subjective;

    @Column(length = 4000)
    private String objective;

    @Column(length = 4000)
    private String assessment;

    @Column(length = 4000)
    private String plan;

    @Column(nullable = false)
    private LocalDateTime capturedAt;

    @Column(length = 100)
    private String capturedBy; // loginId
}
