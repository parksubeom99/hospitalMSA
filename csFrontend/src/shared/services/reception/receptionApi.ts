import { API_BASES } from "@/shared/config/api";
import { fetchJsonWithAuth } from "@/shared/services/_core/http";
import { fromBackendVisitStatus } from '@/shared/lib/integrationBridge';
import { getTodayKST } from '@/shared/lib/date'; // [ADDED] UTC/KST 버그 #3 차단
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
  // [MODIFIED] new Date().toISOString() (UTC) → getTodayKST() (KST)
  // 이유: 한국 자정~오전 9시에 UTC는 전날 날짜 → 오늘 예약 0건 반환 (버그 #3)
  const date = args.date || getTodayKST();
  const payload = await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/reservations?date=${encodeURIComponent(date)}`, { method: 'GET' });
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
  // [FIXED] CANCELED/CANCELLED 상태 visit 목록에서 제거
  // 이유: 삭제(cancel) 후 서버가 CANCELED 반환 → fromBackendVisitStatus가 COMPLETED 변환하지만
  //       목록에 잔류함 → refetch 후에도 안 사라지는 문제
  // 수정: 서버 원본 status가 CANCELED/CANCELLED인 row는 map 전에 미리 제거
  const rows = asArray(payload).filter((x: any) => {
    const raw = String(x.status ?? x.visitStatus ?? '').toUpperCase();
    return raw !== 'CANCELED' && raw !== 'CANCELLED';
  });
  return rows.map((x: any): SyncedVisitRow => {
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
