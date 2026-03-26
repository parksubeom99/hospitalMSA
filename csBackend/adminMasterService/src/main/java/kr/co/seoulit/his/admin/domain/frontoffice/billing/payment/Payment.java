package kr.co.seoulit.his.admin.domain.frontoffice.billing.payment;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_payment", indexes = {
        @Index(name = "idx_admin_payment_invoice", columnList = "invoiceId"),
        @Index(name = "idx_admin_payment_idem", columnList = "idempotencyKey")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private Long invoiceId;

    /** CASH/CARD */
    private String method;

    private Long amount;

    /** PAID */
    private String status;

    /** 중복 결제 방지 키(선택) */
    @Column(unique = true)
    private String idempotencyKey;

    private LocalDateTime paidAt;
}
