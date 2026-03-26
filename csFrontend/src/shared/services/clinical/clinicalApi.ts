import { API_BASES } from "@/shared/config/api";
import { fetchJsonWithAuth } from "@/shared/services/_core/http";
import type { ExamCategory, ExamOrderItem } from '@/shared/types/domain';

const CLINICAL_BASE = API_BASES.clinical;

type HeadersInput = { accessToken?: string; tokenType?: string };

function safeArray(v: any): any[] {
  if (Array.isArray(v)) return v;
  if (Array.isArray(v?.data)) return v.data;
  return [];
}

function categoryLabel(cat: ExamCategory): string {
  return cat === 'LAB' ? '기본검사' : cat === 'RAD' ? '영상검사' : '내시경검사';
}

export async function getSoapServer(args: { session?: HeadersInput; visitId: number }) {
  // [FIXED] 404(SOAP 미작성) = 에러가 아닌 빈 값 → 폼을 빈 상태로 표시
  try {
    const x = await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/emr/soaps/${args.visitId}`, { method: 'GET' });
    return {
      visitId: Number(x.visitId ?? args.visitId),
      subjective: String(x.subjective ?? ''),
      objective: String(x.objective ?? ''),
      assessment: String(x.assessment ?? ''),
      plan: String(x.plan ?? ''),
      updatedAt: String(x.updatedAt ?? new Date().toISOString()),
    };
  } catch (e: any) {
    // 404 = 아직 SOAP 작성 없음 → 빈 값 반환 (정상 케이스)
    const msg = String(e?.message ?? '');
    if (msg.includes('404') || msg.toLowerCase().includes('not found')) {
      return {
        visitId: args.visitId,
        subjective: '',
        objective: '',
        assessment: '',
        plan: '',
        updatedAt: new Date().toISOString(),
      };
    }
    throw e; // 401, 500 등 실제 에러는 그대로 throw
  }
}

export async function saveSoapServer(args: { session?: HeadersInput; visitId: number; soap: { subjective: string; objective: string; assessment: string; plan: string } }) {
  return fetchJsonWithAuth(`${CLINICAL_BASE}/emr/soaps/${args.visitId}`, {
    method: 'PUT', body: JSON.stringify(args.soap),
  });
}

export async function getExamOrdersByVisitServer(args: { session?: HeadersInput; visitId: number }): Promise<ExamOrderItem[]> {
  const headers = safeArray(await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/orders`, { method: 'GET' }));
  const target = headers.filter((h) => Number(h.visitId) === args.visitId && !String(h.status || '').toUpperCase().includes('CANCEL'));
  const out: ExamOrderItem[] = [];
  for (const h of target) {
    const cat = String(h.category || '').toUpperCase();
    if (!(cat === 'LAB' || cat === 'RAD' || cat === 'PROC')) continue;
    const items = safeArray(await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/orders/${h.orderId}/items`, { method: 'GET' }));
    for (const it of items) {
      out.push({
        category: cat as ExamCategory,
        code: String(it.itemCode ?? `${cat}_ITEM`),
        name: String(it.itemName ?? categoryLabel(cat as ExamCategory)),
      });
    }
  }
  return out;
}

async function findExistingOrder(args: { session?: HeadersInput; visitId: number; category: ExamCategory }) {
  const list = safeArray(await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/orders`, { method: 'GET' }));
  return list
    .filter((x) => Number(x.visitId) === args.visitId && String(x.category || '').toUpperCase() === args.category)
    .sort((a, b) => Number(b.orderId || 0) - Number(a.orderId || 0))[0];
}

export async function saveExamOrdersByVisitServer(args: { session?: HeadersInput; visitId: number; items: ExamOrderItem[] }) {
  const byCat: Record<ExamCategory, ExamOrderItem[]> = { LAB: [], RAD: [], PROC: [] };
  args.items.forEach((i) => byCat[i.category].push(i));
  const results: any[] = [];

  for (const cat of ['LAB', 'RAD', 'PROC'] as ExamCategory[]) {
    const items = byCat[cat];
    if (!items.length) continue;

    const existing = await findExistingOrder({ session: args.session, visitId: args.visitId, category: cat });
    if (existing && String(existing.status || '').toUpperCase() === 'NEW') {
      results.push(await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/orders/${existing.orderId}/items`, {
        method: 'PUT',
        body: JSON.stringify({ items: items.map((i) => ({ itemCode: i.code, itemName: i.name, qty: 1 })) }),
      }));
      continue;
    }

    const idempotencyKey = `front-step3-${args.visitId}-${cat}-${Date.now()}`;
    results.push(await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/orders`, {
      method: 'POST',
      body: JSON.stringify({
        visitId: args.visitId,
        category: cat,
        idempotencyKey,
        items: items.map((i) => ({ itemCode: i.code, itemName: i.name, qty: 1 })),
      }),
    }));
  }
  return results;
}

export async function getFinalOrdersByVisitServer(args: { session?: HeadersInput; visitId: number }) {
  const list = safeArray(await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/final-orders`, { method: 'GET' }));
  return list.filter((x) => Number(x.visitId) === args.visitId).sort((a, b) => Number(b.finalOrderId || 0) - Number(a.finalOrderId || 0));
}

export async function saveAndFinalizeFinalOrdersServer(args: { session?: HeadersInput; visitId: number; types: string[]; note?: string }) {
  const saved: any[] = [];
  for (const type of args.types) {
    const created = await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/final-orders`, {
      method: 'POST',
      body: JSON.stringify({
        visitId: args.visitId,
        type,
        note: args.note ?? `front-step3 ${type}`,
        idempotencyKey: `front-final-${args.visitId}-${type}-${Date.now()}`,
      }),
    });
    const id = Number(created.finalOrderId ?? created.id ?? 0);
    if (id > 0) {
      saved.push(await fetchJsonWithAuth<any>(`${CLINICAL_BASE}/final-orders/${id}/finalize`, { method: 'POST' }));
    } else {
      saved.push(created);
    }
  }
  return saved;
}