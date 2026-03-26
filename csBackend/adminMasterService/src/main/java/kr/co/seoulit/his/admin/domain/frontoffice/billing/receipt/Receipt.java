package kr.co.seoulit.his.admin.domain.frontoffice.billing.receipt;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_receipt", indexes = {
        @Index(name = "idx_admin_receipt_payment", columnList = "paymentId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long receiptId;

    private Long paymentId;

    /** RCT-YYYYMMDD-00001 */
    private String receiptNo;

    private LocalDateTime createdAt;
}
