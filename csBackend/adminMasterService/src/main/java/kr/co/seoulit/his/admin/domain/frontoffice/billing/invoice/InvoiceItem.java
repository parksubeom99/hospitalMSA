package kr.co.seoulit.his.admin.domain.frontoffice.billing.invoice;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_invoice_item", indexes = {
        @Index(name = "idx_admin_invoice_item_invoice", columnList = "invoiceId")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceItemId;

    private Long invoiceId;

    private String itemCode;
    private String itemName;

    private Long unitPrice;
    private Integer qty;
    private Long lineTotal;
}
