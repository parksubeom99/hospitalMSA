import { API_BASES } from "@/shared/config/api";
import { fetchJsonWithAuth } from "@/shared/services/_core/http";
import { toBackendVisitStatus } from '@/shared/lib/integrationBridge';
import type { VisitStatus } from '@/shared/types/domain';

type HeadersInput = { accessToken?: string; tokenType?: string };
const ADMIN_BASE = API_BASES.admin;

type BackendInvoiceItem = {
  invoiceItemId?: number;
  itemCode?: string;
  itemName?: string;
  unitPrice?: number;
  qty?: number;
  lineTotal?: number;
};

export type BackendInvoice = {
  invoiceId: number;
  visitId: number;
  status: string;
  totalAmount: number;
  createdAt?: string;
  updatedAt?: string;
  items: BackendInvoiceItem[];
};

export type BackendPayment = {
  paymentId: number;
  invoiceId: number;
  method: string;
  amount: number;
  status: string;
  idempotencyKey?: string;
  paidAt?: string;
};

function asArray(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.items)) return payload.items;
  return [];
}

export async function createInvoiceServer(args: { session?: HeadersInput; visitId: number; amount: number }) {
  const raw = await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/billing/invoices`, {
    method: 'POST',
    body: JSON.stringify({ visitId: args.visitId, amount: Math.max(0, Math.round(args.amount)) }),
  });
  // [FIXED] ApiResponseAdvice 래핑 언래핑
  const x = raw?.data ?? raw;
  return normalizeInvoice(x);
}

export async function listInvoicesServer(args: { session?: HeadersInput; visitId?: number }): Promise<BackendInvoice[]> {
  const q = args.visitId ? `?visitId=${encodeURIComponent(String(args.visitId))}` : '';
  const list = asArray(await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/billing/invoices${q}`, { method: 'GET' }));
  return list.map(normalizeInvoice);
}

export async function payInvoiceServer(args: { session?: HeadersInput; invoiceId: number; method: 'CARD' | 'CASH'; amount: number; idempotencyKey?: string }): Promise<BackendPayment> {
  const raw = await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/billing/payments`, {
    method: 'POST',
    body: JSON.stringify({
      invoiceId: args.invoiceId,
      method: args.method,
      amount: Math.max(0, Math.round(args.amount)),
      idempotencyKey: args.idempotencyKey,
    }),
  });
  // [FIXED] ApiResponseAdvice 래핑 언래핑
  const x = raw?.data ?? raw;
  return {
    paymentId: Number(x.paymentId ?? 0),
    invoiceId: Number(x.invoiceId ?? args.invoiceId),
    method: String(x.method ?? args.method),
    amount: Number(x.amount ?? args.amount),
    status: String(x.status ?? ''),
    idempotencyKey: x.idempotencyKey ? String(x.idempotencyKey) : undefined,
    paidAt: x.paidAt ? String(x.paidAt) : undefined,
  };
}

export async function updateVisitStatusServer(args: { session?: HeadersInput; visitId: number; status: VisitStatus }) {
  return fetchJsonWithAuth(`${ADMIN_BASE}/admin/visits/${args.visitId}/status`, {
    method: 'POST',
    body: JSON.stringify({ status: toBackendVisitStatus(args.status) }),
  });
}

function normalizeInvoice(x: any): BackendInvoice {
  return {
    invoiceId: Number(x.invoiceId ?? x.id ?? 0),
    visitId: Number(x.visitId ?? 0),
    status: String(x.status ?? 'ISSUED'),
    totalAmount: Number(x.totalAmount ?? x.amount ?? 0),
    createdAt: x.createdAt ? String(x.createdAt) : undefined,
    updatedAt: x.updatedAt ? String(x.updatedAt) : undefined,
    items: asArray(x.items).map((it: any) => ({
      invoiceItemId: Number(it.invoiceItemId ?? it.id ?? 0) || undefined,
      itemCode: it.itemCode ? String(it.itemCode) : undefined,
      itemName: it.itemName ? String(it.itemName) : undefined,
      unitPrice: Number(it.unitPrice ?? 0),
      qty: Number(it.qty ?? 1),
      lineTotal: Number(it.lineTotal ?? (Number(it.unitPrice ?? 0) * Number(it.qty ?? 1))),
    })),
  };
}
