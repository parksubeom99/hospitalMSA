package kr.co.seoulit.his.admin.domain.frontoffice.billing.invoice;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_invoice", indexes = {
        @Index(name = "idx_admin_invoice_visit", columnList = "visitId"),
        @Index(name = "idx_admin_invoice_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    private Long visitId;

    /** ISSUED/PAID/CANCELED */
    private String status;

    /** 원 단위 합계(단순 long) */
    private Long totalAmount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
