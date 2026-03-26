import { API_BASES } from "@/shared/config/api";
import { fetchJsonWithAuth } from "@/shared/services/_core/http";
import { fromBackendVisitStatus } from '@/shared/lib/integrationBridge';
import type { VisitStatus } from '@/shared/types/domain';

type HeadersInput = { accessToken?: string; tokenType?: string };
const ADMIN_BASE = API_BASES.admin;

function asArray(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.items)) return payload.items;
  return [];
}

export type SyncedReservationRow = {
  id: number;
  reservedAt: string;
  status: 'RESERVED' | 'CHECKED_IN' | 'CANCELLED';
  contactName: string;
  contactPhone: string;
};

export type SyncedVisitRow = {
  id: number;
  patientName: string;
  gender: 'M' | 'F' | '';
  rrnMasked: string;
  status: VisitStatus;
  registeredAt: string;
  visitType: 'WALK_IN' | 'RESERVATION';
};

export async function fetchReservationsServer(args: { session?: HeadersInput; date?: string }): Promise<SyncedReservationRow[]> {
  const date = args.date || new Date().toISOString().slice(0, 10);
  const payload = await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/reservations?date=${encodeURIComponent(date)}`, { method: 'GET' });
  // [MODIFIED] map 반환 타입 SyncedReservationRow 명시 (이전 오기입 SyncedVisitRow 수정)
  return asArray(payload).map((x: any): SyncedReservationRow => {
    const rawStatus = String(x.status || x.reservationStatus || 'BOOKED').toUpperCase();
    const status: SyncedReservationRow['status'] =
      rawStatus.includes('CHECK') || rawStatus === 'ARRIVED' ? 'CHECKED_IN' : rawStatus.includes('CANCEL') ? 'CANCELLED' : 'RESERVED';
    return {
      id: Number(x.reservationId ?? x.id ?? 0),
      reservedAt: String(x.scheduledAt ?? x.reservedAt ?? x.reservationAt ?? x.createdAt ?? new Date().toISOString()),
      status,
      contactName: String(x.patientName ?? x.reservationName ?? x.name ?? '-'),
      contactPhone: String(x.phone ?? x.contactPhone ?? '-'),
    };
  }).filter((r: SyncedReservationRow) => Number.isFinite(r.id) && r.id > 0);
}

export async function fetchVisitsServer(args: { session?: HeadersInput; statuses?: string[] }): Promise<SyncedVisitRow[]> {
  const query = args.statuses?.length ? `?status=${encodeURIComponent(args.statuses.join(','))}` : '';
  const payload = await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/visits${query}`, { method: 'GET' });
  // [MODIFIED] gender를 변수로 분리해 SyncedVisitRow['gender'] 타입 명시 → string 넓힘 방지
  // map 반환 타입 SyncedVisitRow 명시로 함수 반환 타입 완전 일치
  return asArray(payload).map((x: any): SyncedVisitRow => {
    const backendStatus = String(x.status ?? x.visitStatus ?? 'WAITING');
    const registeredAt = String(x.arrivedAt ?? x.registeredAt ?? x.createdAt ?? new Date().toISOString());
    const rawType = String(x.visitType ?? x.arrivalType ?? 'WALK_IN').toUpperCase();
    const genderRaw = String(x.gender ?? '').toUpperCase();
    const gender: SyncedVisitRow['gender'] = genderRaw === 'M' ? 'M' : genderRaw === 'F' ? 'F' : '';
    return {
      id: Number(x.visitId ?? x.id ?? 0),
      patientName: String(x.patientName ?? x.name ?? x.reservationName ?? '-'),
      gender,
      rrnMasked: String(x.rrnMasked ?? x.maskedRrn ?? '******-*******'),
      status: fromBackendVisitStatus(backendStatus),
      registeredAt,
      visitType: rawType.includes('RESERV') ? 'RESERVATION' : 'WALK_IN',
    };
  }).filter((v: SyncedVisitRow) => Number.isFinite(v.id) && v.id > 0);
}
