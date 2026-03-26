import type { FinalOrderDraft, InvoiceItem } from "@/shared/types/domain";

export function buildInvoiceItems(finalOrder: FinalOrderDraft | undefined): InvoiceItem[] {
  if (!finalOrder) return [];
  const items: InvoiceItem[] = [];
  const hasNone = finalOrder.types.includes("NONE");
  if (hasNone) return [];

  if (finalOrder.types.includes("MED")) {
    const qty = finalOrder.medications.reduce((sum, m) => sum + Number(m.qty || 0), 0);
    if (qty > 0) {
      items.push({
        itemType: "MED",
        itemName: "약제비",
        qty,
        unitPrice: 1000,
        amount: qty * 1000,
      });
    }
  }


  if (finalOrder.types.includes("INJECTION")) {
    const qty = (finalOrder.injections ?? []).length;
    if (qty > 0) {
      items.push({
        itemType: "INJECTION",
        itemName: "주사비",
        qty,
        unitPrice: 5000,
        amount: qty * 5000,
      });
    }
  }

  if (finalOrder.types.includes("SURGERY") && finalOrder.surgery) {
    const unitPrice = finalOrder.surgery.surgeryType === "INTERNAL" ? 50000 : 100000;
    items.push({
      itemType: "SURGERY",
      itemName: finalOrder.surgery.surgeryType === "INTERNAL" ? "내과수술" : "외과수술",
      qty: 1,
      unitPrice,
      amount: unitPrice,
      metaLabel: `수술실 ${finalOrder.surgery.roomNo}번`,
    });
  }

  if (finalOrder.types.includes("ADMISSION") && finalOrder.admission) {
    const qty = finalOrder.admission.nights;
    if (qty > 0) {
      items.push({
        itemType: "ADMISSION",
        itemName: "입원비",
        qty,
        unitPrice: 10000,
        amount: qty * 10000,
        metaLabel: `병동 ${finalOrder.admission.wardNo}번`,
      });
    }
  }

  return items;
}

export function totalAmount(items: InvoiceItem[]): number {
  return items.reduce((sum, item) => sum + item.amount, 0);
}
