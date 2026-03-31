// [REMOVED] export * from "../master/masterStaffApi"
// 이유: 자기 자신을 re-export하는 순환 참조 제거 (번들러 경고/무한 해석 위험)
import { API_BASES } from "@/shared/config/api";
import { apiFetchWithAuth } from "@/shared/services/authApi";
import type { StaffProfile } from "@/shared/types/domain";

const ADMIN_BASE = API_BASES.admin;

type BackendStaff = {
  staffProfileId?: number;
  loginId?: string;
  name?: string;
  jobType?: string;
  departmentId?: number | null;
  phone?: string;
  email?: string;
  active?: boolean;
};

type DepartmentDto = {
  departmentId?: number;
  code?: string;
  name?: string;
  active?: boolean;
};

// [NEW] 백엔드 공통 래핑 응답 타입 (ApiResponse<T>)
type ApiResponse<T> = {
  success?: boolean;
  data?: T;
  error?: unknown;
};

function safeJsonParse(text: string): unknown {
  try { return JSON.parse(text); } catch { return text; }
}

// [FIX] unwrapData: 백엔드가 { success, data, error } 구조로 래핑할 경우 data 필드 추출
//       배열 직접 응답도 그대로 처리 (하위 호환)
function unwrapData<T>(raw: unknown): T {
  if (Array.isArray(raw)) return raw as T;
  if (raw && typeof raw === "object" && "data" in (raw as any)) {
    return ((raw as any).data ?? []) as T;
  }
  return raw as T;
}

async function fetchJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers ?? {});
  if (!headers.has("Content-Type") && init.method && init.method !== "GET") {
    headers.set("Content-Type", "application/json");
  }
  const res = await apiFetchWithAuth(`${ADMIN_BASE}${path}`, { ...init, headers, cache: "no-store" });
  const text = await res.text().catch(() => "");
  const data = text ? safeJsonParse(text) : undefined;
  if (!res.ok) {
    const msg = typeof data === "object" && data && "message" in (data as any)
      ? String((data as any).message)
      : text || `HTTP ${res.status}`;
    if (res.status === 401) throw new Error("실서버 세션이 만료되었거나 토큰이 없습니다. 다시 로그인해주세요.");
    if (res.status === 403) throw new Error("실서버 권한이 없거나 데모 토큰으로 호출했습니다. IAM 실로그인을 다시 확인해주세요.");
    throw new Error(msg);
  }
  return data as T;
}

function normJobType(raw: unknown): StaffProfile["jobType"] {
  const s = String(raw || "").toUpperCase();
  return s.includes("DOC") || s.includes("DOCTOR") ? "DOCTOR" : "ADMIN";
}

function inferDepartmentName(jobType: StaffProfile["jobType"], departmentId?: number | null, deptMap?: Map<number, string>): string {
  if (jobType === "ADMIN") return "원무과";
  if (departmentId && deptMap?.has(departmentId)) return String(deptMap.get(departmentId));
  // [MODIFIED] fallback: departmentId로 하드코딩 매핑 (deptMap 없을 때도 정확한 부서명 반환)
  if (departmentId === 1) return "내과";
  if (departmentId === 2) return "외과";
  if (departmentId === 3) return "영상의학과";
  return "내과";
}

function fallbackPhone(id: number) {
  return `010-${String((1000 + (id % 9000))).slice(-4)}-${String((2000 + (id % 8000))).slice(-4)}`;
}

function mapBackendToUi(x: BackendStaff, deptMap?: Map<number, string>): StaffProfile {
  const staffId = Number(x.staffProfileId ?? 0);
  const jobType = normJobType(x.jobType);
  const loginId = String(x.loginId ?? `staff${staffId}`);
  return {
    staffId,
    // [MODIFIED] name이 비어있을 때 공백 대신 빈 문자열로 명시적 처리
    staffName: String(x.name ?? "").trim(),
    jobType,
    department: inferDepartmentName(jobType, x.departmentId ?? undefined, deptMap),
    specialty: "",
    phone: String(x.phone ?? fallbackPhone(staffId || 1)),
    email: String(x.email ?? `${loginId}@hospital.local`),
    active: Boolean(x.active ?? true),
  };
}

export async function fetchDepartmentsServer(): Promise<Map<number, string>> {
  const raw = await fetchJson<unknown>(`/master/departments`, { method: "GET" });
  // [FIX] 부서 목록도 래핑 응답 unwrap 적용
  const rows = unwrapData<DepartmentDto[]>(raw);
  const map = new Map<number, string>();
  (Array.isArray(rows) ? rows : []).forEach((d) => {
    const id = Number(d.departmentId ?? 0);
    if (id > 0) map.set(id, String(d.name ?? d.code ?? `부서${id}`));
  });
  return map;
}

// 부서 역방향 캐시 (name → departmentId)
// listMasterStaffServer() 호출 시 자동 구축, toDepartmentIdByName()에서 우선 사용
let _deptNameToIdCache: Map<string, number> | null = null;

// [ADDED] deptMap 정방향 캐시 (departmentId → name) — update 시 재사용
let _deptIdToNameCache: Map<number, string> | null = null;

export async function listMasterStaffServer(): Promise<StaffProfile[]> {
  let deptMap: Map<number, string> | undefined;
  try {
    deptMap = await fetchDepartmentsServer();
    // [MODIFIED] 정방향/역방향 캐시 모두 갱신
    _deptIdToNameCache = deptMap;
    _deptNameToIdCache = new Map<string, number>();
    deptMap.forEach((name, id) => {
      _deptNameToIdCache!.set(name, id);
    });
  } catch { deptMap = undefined; }

  const raw = await fetchJson<unknown>(`/master/staff`, { method: "GET" });
  // [FIX] 백엔드 { success, data: [...] } 래핑 응답 unwrap
  const rows = unwrapData<BackendStaff[]>(raw);
  return (Array.isArray(rows) ? rows : []).map((x) => mapBackendToUi(x, deptMap)).filter((s) => s.staffId > 0);
}

function toDepartmentIdByName(department: string, jobType: StaffProfile["jobType"]): number | null {
  if (jobType === "ADMIN") return null;
  const d = String(department || "");
  if (_deptNameToIdCache) {
    for (const [name, id] of _deptNameToIdCache.entries()) {
      if (d.includes(name) || name.includes(d)) return id;
    }
  }
  // [MODIFIED] fallback 하드코딩도 영상의학과 포함하여 3개 완전 명시
  if (d.includes("내과")) return 1;
  if (d.includes("외과")) return 2;
  if (d.includes("영상")) return 3;
  return null;
}

// [FIX] buildLoginId — Date.now() 제거
// 기존: 매 호출마다 loginId가 달라져 백엔드 upsert가 항상 신규 row 생성
// 변경: jobType prefix + staffName 고정 조합 → 동일 직원은 항상 동일 loginId → UPDATE 정상 동작
function buildLoginId(staff: StaffProfile): string {
  const prefix = staff.jobType === "DOCTOR" ? "doc" : "adm";
  const name = staff.staffName
    .toLowerCase()
    .replace(/[^a-z0-9가-힣]/g, "")
    .slice(0, 12);
  return `${prefix}.${name || "staff"}`;
}

export async function createMasterStaffServer(staff: StaffProfile): Promise<StaffProfile> {
  const payload = {
    loginId: buildLoginId(staff),
    name: staff.staffName,
    jobType: staff.jobType,
    departmentId: toDepartmentIdByName(staff.department, staff.jobType),
    phone: staff.phone,
    email: staff.email,
    active: staff.active ?? true,
  };
  const raw = await fetchJson<unknown>(`/master/staff`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
  // [FIX] POST 응답도 unwrap 적용
  const saved = unwrapData<BackendStaff>(raw);
  // [MODIFIED] POST 응답에도 deptIdToNameCache 전달 → 등록 직후 부서명 정확히 반영
  return mapBackendToUi(saved as BackendStaff, _deptIdToNameCache ?? undefined);
}

export async function updateMasterStaffServer(staff: StaffProfile): Promise<StaffProfile> {
  // [MODIFIED] name 공백 방어: 빈 이름으로 @NotBlank 위반 방지
  const safeName = staff.staffName?.trim() || "";
  if (!safeName) throw new Error("직원 이름이 비어 있어 저장할 수 없습니다.");

  const payload = {
    name: safeName,
    jobType: staff.jobType,
    departmentId: toDepartmentIdByName(staff.department, staff.jobType),
    phone: staff.phone,
    email: staff.email,
    active: staff.active,
  };
  const raw = await fetchJson<unknown>(`/master/staff/${staff.staffId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
  // [FIX] PUT 응답도 unwrap 적용
  const saved = unwrapData<BackendStaff>(raw);
  // [MODIFIED] _deptIdToNameCache를 전달 → 수정 후 화면에 올바른 부서명 표시
  //            기존: mapBackendToUi(saved, undefined) → 항상 내과 fallback
  //            변경: mapBackendToUi(saved, _deptIdToNameCache) → departmentId로 정확한 부서명 매핑
  return mapBackendToUi(saved as BackendStaff, _deptIdToNameCache ?? undefined);
}

export async function deactivateMasterStaffServer(staffId: number): Promise<void> {
  await fetchJson<void>(`/master/staff/${staffId}`, { method: "DELETE" });
}