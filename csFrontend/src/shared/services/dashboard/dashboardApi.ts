import { API_BASES } from "@/shared/config/api";
import { fetchJsonWithAuth } from "@/shared/services/_core/http";

type HeadersInput = { accessToken?: string; tokenType?: string };
const ADMIN_BASE = API_BASES.admin;

export type DashboardSummaryServer = {
  counts: {
    waiting: number;
    reservation: number;
    emergency: number;
  };
  patients: Array<{
    visitId: number;
    patientId?: number;
    patientName?: string;
    departmentCode?: string;
    doctorId?: string;
    status?: string;
    arrivalType?: string;
    triageLevel?: number;
    createdAt?: string;
  }>;
  generatedAt?: string;
};

export async function fetchDashboardSummaryServer(args?: { session?: HeadersInput; date?: string }): Promise<DashboardSummaryServer> {
  // [MODIFIED] UTC→KST 날짜 보정 (dashboardQueries에서 이미 KST date를 전달하므로 fallback용)
  const kstMs = Date.now() + 9 * 60 * 60 * 1000;
  const date = args?.date || new Date(kstMs).toISOString().slice(0, 10);
  const raw = await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/dashboard/summary?date=${encodeURIComponent(date)}`, {
    method: "GET",
  });
  // [FIXED] adminMasterService는 ApiResponseAdvice로 모든 응답을 {success, data, ...}로 래핑함
  //         raw.data가 있으면 언래핑, 없으면 raw 그대로 사용 (방어적 처리)
  const x = raw?.data ?? raw;
  return {
    counts: {
      waiting: Number(x?.counts?.waiting ?? 0),
      reservation: Number(x?.counts?.reservation ?? 0),
      emergency: Number(x?.counts?.emergency ?? 0),
    },
    patients: Array.isArray(x?.patients)
      ? x.patients.map((p: any) => ({
          visitId: Number(p.visitId ?? p.id ?? 0),
          patientId: p.patientId != null ? Number(p.patientId) : undefined,
          patientName: p.patientName != null ? String(p.patientName) : undefined,
          departmentCode: p.departmentCode != null ? String(p.departmentCode) : undefined,
          doctorId: p.doctorId != null ? String(p.doctorId) : undefined,
          status: p.status != null ? String(p.status) : undefined,
          arrivalType: p.arrivalType != null ? String(p.arrivalType) : undefined,
          triageLevel: p.triageLevel != null ? Number(p.triageLevel) : undefined,
          createdAt: p.createdAt != null ? String(p.createdAt) : undefined,
        })).filter((p: any) => Number.isFinite(p.visitId) && p.visitId > 0)
      : [],
    generatedAt: x?.generatedAt ? String(x.generatedAt) : undefined,
  };
}

// ─────────────────────────────────────────────────────────────
// 응급 병상 수 API (admin_config 기반)
// ─────────────────────────────────────────────────────────────

export async function fetchEmergencyCount(): Promise<number> {
  const raw = await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/config/emergency-count`, {
    method: "GET",
  });
  const x = raw?.data ?? raw;
  return Number(x?.value ?? 3);
}

export async function updateEmergencyCount(value: number): Promise<number> {
  const raw = await fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/config/emergency-count`, {
    method: "PUT",
    body: JSON.stringify({ value }),
  });
  const x = raw?.data ?? raw;
  return Number(x?.value ?? value);
}
