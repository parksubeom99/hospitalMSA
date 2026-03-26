package kr.co.seoulit.his.admin.domain.frontoffice.billing.invoice;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_invoice_adjustment", indexes = {
        @Index(name = "idx_inv_adj_invoice", columnList = "invoiceId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adjustmentId;

    private Long invoiceId;

    /** ADJUST/CANCEL */
    private String type;

    private Integer oldAmount;
    private Integer newAmount;

    private String reason;

    private LocalDateTime createdAt;
}
