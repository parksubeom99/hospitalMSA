"use client";

import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { DEFAULT_UNMASK_REASON, MAX_EMERGENCY_COUNT, MAX_TOTAL_CAPACITY } from "@/shared/config/constants";
import { getCapacityLevel } from "@/shared/lib/capacity";
import { calcNights } from "@/shared/lib/date";
import { buildInvoiceItems, totalAmount } from "@/shared/lib/price";
import { isManualVisitTransitionAllowed, normalizeRoleCode } from "@/shared/lib/integrationBridge";
import { loginAuth, logoutAuth, meAuth } from "@/shared/services/authApi";
import { clearStoredAuthTokens, loadStoredAuthTokens, saveStoredAuthTokens } from "@/shared/services/tokenStorage";
import type {
  CapacitySummary,
  DemoState,
  ExamCategory,
  ExamOrderItem,
  FinalOrderAdmission,
  FinalOrderDraft,
  MedicationCatalogItem,
  Patient,
  RoleCode,
  SessionState,
  StaffProfile,
  UserSession,
  VisitStatus,
} from "@/shared/types/domain";

// [B-1] 상태 일원화 — 영속 키 v3
// v2는 { session, ...domain } 단일 평면 구조였으나, v3부터 { session, demo } 2-슬라이스 구조.
// 키를 올려 구버전 데이터와의 형상 충돌을 차단(구버전 데이터는 무시되고 seed로 시작).
const STORAGE_KEY = "hospital-msa-front-4svc-v3";

export const MEDICATION_CATALOG: MedicationCatalogItem[] = [
  { drugCode: "DRUG001", drugName: "타이레놀정", drugGroup: "진통제", unitPrice: 1000 },
  { drugCode: "DRUG002", drugName: "이부프로펜정", drugGroup: "소염진통제", unitPrice: 1000 },
  { drugCode: "DRUG003", drugName: "록소프로펜정", drugGroup: "소염진통제", unitPrice: 1000 },
  { drugCode: "DRUG004", drugName: "알마겔현탁액", drugGroup: "소화제", unitPrice: 1000 },
  { drugCode: "DRUG005", drugName: "모사프리드정", drugGroup: "소화제", unitPrice: 1000 },
  { drugCode: "DRUG006", drugName: "디아제팜정", drugGroup: "안정제", unitPrice: 1000 },
  { drugCode: "DRUG007", drugName: "세티리진정", drugGroup: "항히스타민제", unitPrice: 1000 },
  { drugCode: "DRUG008", drugName: "레보플록사신정", drugGroup: "항생제", unitPrice: 1000 },
  { drugCode: "DRUG009", drugName: "아세틸시스테인정", drugGroup: "거담제", unitPrice: 1000 },
  { drugCode: "DRUG010", drugName: "티아민정", drugGroup: "비타민", unitPrice: 1000 },
];

const now = () => new Date().toISOString();

function toBase64UrlUtf8(value: unknown): string {
  const json = typeof value === "string" ? value : JSON.stringify(value);
  const bytes = new TextEncoder().encode(json);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function makeMockJwt(payload: Record<string, unknown>): string {
  const h = toBase64UrlUtf8({ alg: "HS256", typ: "JWT" });
  const b = toBase64UrlUtf8(payload);
  return `${h}.${b}.mock-signature`;
}

const MOCK_ACCOUNTS = [
  { username: "sys123", password: "system", role: "SYS" as const, displayName: "시스템관리자" },
  { username: "admin123", password: "administration", role: "ADMIN" as const, displayName: "원무직원" },
  { username: "park123", password: "doctor", role: "DOC" as const, displayName: "박혁거세 의사", doctorStaffId: 3 },
  { username: "kim123", password: "doctor", role: "DOC" as const, displayName: "김시민 의사", doctorStaffId: 2 },
  { username: "lee123", password: "doctor", role: "DOC" as const, displayName: "이순신 의사", doctorStaffId: 1 },
];

// [B-1] createSeedState → createSeedDemoState
// 데모 슬라이스 전용 시드. session은 더 이상 이 슬라이스에 포함되지 않는다.
function createSeedDemoState(): DemoState {
  // [MODIFIED] new Date() → 고정 날짜 문자열
  // 이유: SSR(서버) 실행 시각 ≠ CSR(클라이언트) hydration 시각 → React #425/#418/#423 hydration mismatch
  // 해결: seed 데이터는 고정 ISO 문자열 사용, 런타임 now()는 액션 시점에만 호출
  const SEED_DATE = "2026-01-15";
  const at = (h: number, m: number) => {
    const hh = String(h).padStart(2, "0");
    const mm = String(m).padStart(2, "0");
    return `${SEED_DATE}T${hh}:${mm}:00.000Z`;
  };
  void at;
  return {
    emergencyCount: 3,
    // [MODIFIED v3] 서버 시드 축소(reservation/visit INSERT 제거)에 맞춰 로컬도 빈 상태.
    // patients: 서버 seed_demo_reset.sql v3.1과 동일한 5명 (성/이름 모두 distinct)
    // reservations/visits/soaps: 빈 상태 — 시연 중 라이브로만 생성, 유령 데이터 차단
    patients: [
      { id: 2001, name: "박서준", gender: "M", rrnFront: "950315", rrnBack: "1111111", phone: "010-1234-5678" },
      { id: 2002, name: "이하늘", gender: "F", rrnFront: "880722", rrnBack: "2222222", phone: "010-2345-6789" },
      { id: 2003, name: "김도윤", gender: "M", rrnFront: "010910", rrnBack: "3333333", phone: "010-3456-7890" },
      { id: 2004, name: "정유나", gender: "F", rrnFront: "970205", rrnBack: "2444444", phone: "010-4567-8901" },
      { id: 2005, name: "최예은", gender: "F", rrnFront: "920830", rrnBack: "2555555", phone: "010-5678-9012" },
    ],
    reservations: [],
    visits: [],
    soaps: {},
    examOrders: {},
    finalOrders: {},
    invoices: [],
    staff: [
      { staffId: 1, staffName: "이순신", jobType: "DOCTOR", department: "내과", specialty: "소화기", phone: "010-1111-1111", email: "lee@hospital.local", active: true },
      { staffId: 2, staffName: "김시민", jobType: "DOCTOR", department: "외과", specialty: "일반외과", phone: "010-2222-2222", email: "kim@hospital.local", active: true },
      { staffId: 3, staffName: "박혁거세", jobType: "DOCTOR", department: "영상의학과", specialty: "영상판독", phone: "010-3333-3333", email: "park@hospital.local", active: true },
      { staffId: 4, staffName: "원무1", jobType: "ADMIN", department: "원무과", specialty: "", phone: "010-4444-4444", email: "adm1@hospital.local", active: true },
      { staffId: 5, staffName: "원무2", jobType: "ADMIN", department: "원무과", specialty: "", phone: "010-5555-5555", email: "adm2@hospital.local", active: true },
    ],
    rrnUnmaskAudit: [],
  };
}

interface ActionResult {
  ok: boolean;
  message: string;
}

interface HospitalContextValue {
  // [ADDED] hydrated: localStorage 복원 + 토큰 유효성 검증 완료 여부
  // SSR(false) → client useEffect 완료(true)
  // RoleGate가 이 값을 보고 hydration 전 렌더를 억제 → #418/#423/#425 해소
  hydrated: boolean;
  // [B-1] state: 세션 슬라이스 ({ session }) — 서버·데모 공통
  state: SessionState;
  // [B-1] demo: 데모 모드 전용 격리 슬라이스 — 서버 모드 화면은 참조 금지
  demo: DemoState;
  capacity: CapacitySummary;
  medicationCatalog: MedicationCatalogItem[];
  loginAs: (role: RoleCode) => void;
  loginWithCredentials: (payload: { username: string; password: string }) => ActionResult;
  loginWithServerCredentials: (payload: { username: string; password: string }) => Promise<ActionResult>;
  bootstrapAuthSession: () => Promise<ActionResult>;
  logout: () => void;
  resetDemoData: () => void;
  setEmergencyCount: (value: number) => ActionResult;
  addReservation: (payload: { patientId: number; reservedAt: string; memo?: string }) => ActionResult;
  createReservationEntry: (payload: { name: string; phone: string; reservedAt: string }) => ActionResult;
  updateReservationEntry: (reservationId: number, payload: { name: string; phone: string; reservedAt: string }) => ActionResult;
  addVisit: (payload: { patientId: number; visitType: "WALK_IN" | "RESERVATION" }) => ActionResult;
  registerVisitEntry: (payload: { mode: "WALK_IN" | "RESERVATION"; reservationId?: number; patientName: string; gender: "M" | "F"; rrnFront: string; rrnBack: string; phone: string }) => ActionResult;
  updateVisitEntry: (visitId: number, payload: { patientName: string; gender: "M" | "F"; rrnFront: string; rrnBack: string; phone: string; status: VisitStatus }) => ActionResult;
  removeVisitEntry: (visitId: number) => ActionResult;
  updateVisitStatus: (visitId: number, status: VisitStatus) => ActionResult;
  getUnmaskedRrn: (visitId: number, reason?: string) => { ok: boolean; rrn?: string; message: string };
  saveSoap: (visitId: number, data: { subjective: string; objective: string; assessment: string; plan: string }) => ActionResult;
  saveExamOrders: (visitId: number, items: ExamOrderItem[]) => ActionResult;
  saveFinalOrder: (draft: Omit<FinalOrderDraft, "updatedAt">) => ActionResult;
  generateInvoiceFromFinalOrder: (visitId: number) => ActionResult;
  payInvoice: (invoiceId: number, method: "CARD" | "CASH") => ActionResult;
  upsertStaff: (staff: StaffProfile) => ActionResult;
  removeStaff: (staffId: number) => ActionResult;
  patientsById: Record<number, Patient>;
}

const HospitalContext = createContext<HospitalContextValue | null>(null);

function buildCapacity(demo: DemoState): CapacitySummary {
  const waitingAndInTreatment = demo.visits.filter(v => !v.cancelled && (v.status === "WAITING" || v.status === "IN_TREATMENT")).length;
  const reservation = demo.reservations.filter(r => r.status === "RESERVED").length;
  const emergency = demo.emergencyCount;
  const current = waitingAndInTreatment + reservation + emergency;
  const level = getCapacityLevel(current, MAX_TOTAL_CAPACITY);
  return {
    max: MAX_TOTAL_CAPACITY,
    current,
    level,
    canRegister: current < MAX_TOTAL_CAPACITY,
    waitingAndInTreatment,
    reservation,
    emergency,
  };
}

export function HospitalProvider({ children }: { children: React.ReactNode }) {
  // [B-1] 단일 Provider 내부 2-슬라이스 분리
  // session: 인증 상태 (서버·데모 공통) / demo: 데모 모드 전용 도메인 데이터
  const [session, setSession] = useState<UserSession | null>(null);
  const [demo, setDemo] = useState<DemoState>(createSeedDemoState);
  // [MODIFIED] hydrated 의미 확장:
  // 이전: localStorage 복원 완료
  // 변경: localStorage 복원 + 서버 세션 토큰 유효성 검증 완료
  // 이유: 복원 즉시 hydrated=true → isServerSession=true → 만료 토큰으로 API 401 루프 발생
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    // [MODIFIED] 비동기 토큰 검증 포함 초기화 흐름
    // 1. localStorage 복원 ({ session, demo } 2-슬라이스)
    // 2. authSource="server" 세션이 있으면 /auth/me 호출로 토큰 유효성 검증
    //    → 유효: 세션 유지
    //    → 만료/실패: session=null + clearStoredAuthTokens() (401 루프 차단)
    // 3. 완료 후 hydrated=true (성공/실패 무관)
    const init = async () => {
      try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return;
        const parsed = JSON.parse(raw) as { session: UserSession | null; demo: DemoState };
        if (!parsed?.demo?.patients || !parsed?.demo?.visits) return;

        // [B-1] 데모 슬라이스 복원 (rrnUnmaskAudit roleCode 정규화 포함)
        setDemo({
          ...parsed.demo,
          rrnUnmaskAudit: (parsed.demo.rrnUnmaskAudit ?? []).map((a) => ({
            ...a,
            roleCode: (normalizeRoleCode((a as any).roleCode) ?? "SYS") as RoleCode,
          })),
        });

        // [B-1] 세션 슬라이스 복원
        const normalizedSession: UserSession | null = parsed.session
          ? {
              ...parsed.session,
              role: (normalizeRoleCode((parsed.session as any).role) ?? "SYS") as RoleCode,
              authSource: parsed.session.authSource === "server" ? "server" : "demo",
            }
          : null;

        // [ADDED] 실서버 세션 토큰 유효성 검증
        // authSource="server" 복원 시 /auth/me 호출 → 실패면 즉시 세션 초기화
        // 이유: 만료된 토큰을 그대로 복원하면 페이지 진입 즉시 401 루프 발생
        if (normalizedSession?.authSource === "server") {
          const tokens = loadStoredAuthTokens();
          if (tokens?.accessToken) {
            try {
              // [ADDED] 5초 타임아웃 — IAM 서버 다운 시 앱 전체 빈 화면 방지
              const timeout = new Promise<never>((_, reject) =>
                setTimeout(() => reject(new Error("meAuth timeout")), 5000)
              );
              const me = await Promise.race([meAuth(), timeout]);
              // 토큰 유효 → 세션 최신 정보로 갱신 후 유지
              setSession({
                ...normalizedSession,
                role: me.role,
                displayName: me.displayName,
                username: me.username,
                accessToken: tokens.accessToken,
                tokenType: "Bearer",
                authSource: "server",
                doctorStaffId: me.staffId,
              });
              return; // finally에서 hydrated=true 처리
            } catch {
              // [ADDED] 토큰 만료/검증 실패 → 세션 초기화 (401 루프 차단)
              clearStoredAuthTokens();
              setSession(null);
              return;
            }
          } else {
            // 토큰 자체 없음 → 세션 초기화
            setSession(null);
            return;
          }
        }

        // demo 세션 또는 session=null → 그대로 복원
        setSession(normalizedSession);
      } catch {
        // localStorage 파싱 오류 → 기본 seed 상태 유지
      } finally {
        // [UNCHANGED] 성공/실패 무관하게 항상 hydrated=true
        setHydrated(true);
      }
    };

    void init();
  }, []);

  useEffect(() => {
    // [B-1] { session, demo } 2-슬라이스 영속. accessToken은 제외(보안).
    try {
      const persistableSession = session ? { ...session, accessToken: undefined } : null;
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ session: persistableSession, demo }));
    } catch {
      // storage may be unavailable
    }
  }, [session, demo]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const tokens = loadStoredAuthTokens();
    if (!tokens?.accessToken) return;
    setSession((prev) => {
      if (!prev) return prev;
      return {
        ...prev,
        accessToken: tokens.accessToken,
        tokenType: "Bearer",
      };
    });
  }, []);

  const capacity = useMemo(() => buildCapacity(demo), [demo]);

  const patientsById = useMemo(() => {
    return Object.fromEntries(demo.patients.map((p) => [p.id, p]));
  }, [demo.patients]);

  // [B-1] 세션 슬라이스를 안정 참조로 노출 ({ session })
  const sessionSlice = useMemo<SessionState>(() => ({ session }), [session]);

  const guardedCapacity = (delta: number): ActionResult => {
    const next = capacity.current + delta;
    if (next > MAX_TOTAL_CAPACITY) return { ok: false, message: `총 운영 인원 30명 제한 초과 (현재 ${capacity.current}명)` };
    if (next < 0) return { ok: false, message: "인원 수가 0 미만이 될 수 없습니다." };
    return { ok: true, message: "OK" };
  };

  const value: HospitalContextValue = {
    hydrated, // [MODIFIED] localStorage 복원 + 토큰 검증 완료 후 true
    state: sessionSlice,
    demo,
    capacity,
    medicationCatalog: MEDICATION_CATALOG,
    patientsById,
    loginAs: (role) => {
      const displayName = role === "DOC" ? "의사 계정" : role === "ADMIN" ? "원무 계정" : "시스템관리자";
      const token = makeMockJwt({ sub: role, role, name: displayName, iat: Date.now() });
      saveStoredAuthTokens({ accessToken: token, tokenType: "Bearer" });
      setSession({ role, displayName, username: role.toLowerCase(), accessToken: token, tokenType: "Bearer", authSource: "demo" });
    },
    loginWithCredentials: ({ username, password }) => {
      const account = MOCK_ACCOUNTS.find((a) => a.username === username && a.password === password);
      if (!account) return { ok: false, message: "아이디 또는 비밀번호가 올바르지 않습니다." };
      const token = makeMockJwt({ sub: account.username, role: account.role, name: account.displayName, iat: Date.now() });
      saveStoredAuthTokens({ accessToken: token, tokenType: "Bearer" });
      setSession({
        role: account.role,
        displayName: account.displayName,
        username: account.username,
        accessToken: token,
        tokenType: "Bearer",
        authSource: "demo",
        doctorStaffId: (account as any).doctorStaffId,
      });
      return { ok: true, message: `${account.displayName} 로그인 성공` };
    },
    loginWithServerCredentials: async ({ username, password }) => {
      try {
        const result = await loginAuth({ username, password });
        const me = await meAuth().catch(() => result.user);
        saveStoredAuthTokens({
          accessToken: result.token.accessToken,
          refreshToken: result.token.refreshToken,
          tokenType: result.token.tokenType,
        });
        setSession({
          role: me.role,
          displayName: me.displayName,
          username: me.username || result.user.username,
          accessToken: result.token.accessToken,
          tokenType: result.token.tokenType,
          authSource: "server",
          doctorStaffId: me.staffId,
        });
        return { ok: true, message: `${me.displayName} 로그인 성공 (실서버)` };
      } catch (e) {
        const message = e instanceof Error ? e.message : "로그인 실패";
        return { ok: false, message };
      }
    },
    bootstrapAuthSession: async () => {
      const tokens = loadStoredAuthTokens();
      if (!tokens?.accessToken) return { ok: false, message: "저장된 토큰 없음" };
      try {
        const me = await meAuth();
        setSession({
          role: me.role,
          displayName: me.displayName,
          username: me.username,
          accessToken: tokens.accessToken,
          tokenType: "Bearer",
          authSource: "server",
          doctorStaffId: me.staffId,
        });
        return { ok: true, message: "저장된 로그인 세션 복원 완료" };
      } catch (e) {
        clearStoredAuthTokens();
        setSession(null);
        return { ok: false, message: e instanceof Error ? e.message : "세션 복원 실패" };
      }
    },
    logout: () => { void logoutAuth().catch(() => clearStoredAuthTokens()); setSession(null); },
    // [B-1] 데모 데이터 초기화 — 세션도 함께 해제(기존 동작 보존)
    resetDemoData: () => { setDemo(createSeedDemoState()); setSession(null); },
    setEmergencyCount: (value) => {
      if (!Number.isInteger(value) || value < 0 || value > MAX_EMERGENCY_COUNT) {
        return { ok: false, message: "응급 환자 수는 0~10 범위여야 합니다." };
      }
      const delta = value - demo.emergencyCount;
      const cap = guardedCapacity(delta);
      if (!cap.ok) return cap;
      setDemo((prev) => ({ ...prev, emergencyCount: value }));
      return { ok: true, message: `응급 카운터를 ${value}명으로 변경했습니다.` };
    },
    addReservation: ({ patientId, reservedAt, memo }) => {
      const patient = demo.patients.find((p) => p.id === patientId);
      const cap = guardedCapacity(1);
      if (!cap.ok) return cap;
      const nextId = Math.max(500, ...demo.reservations.map(r => r.id)) + 1;
      setDemo((prev) => ({
        ...prev,
        reservations: [...prev.reservations, { id: nextId, patientId, reservedAt, status: "RESERVED", memo, contactName: patient?.name, contactPhone: patient?.phone }],
      }));
      return { ok: true, message: "예약 등록 완료" };
    },
    createReservationEntry: ({ name, phone, reservedAt }) => {
      const cap = guardedCapacity(1);
      if (!cap.ok) return cap;
      if (!name.trim() || !phone.trim() || !reservedAt) return { ok: false, message: "이름/전화번호/예약시간대를 입력해주세요." };
      const nextPatientId = Math.max(2000, ...demo.patients.map((p) => p.id)) + 1;
      const nextReservationId = Math.max(500, ...demo.reservations.map((r) => r.id)) + 1;
      setDemo((prev) => ({
        ...prev,
        patients: [...prev.patients, { id: nextPatientId, name: name.trim(), gender: "M", rrnFront: "000000", rrnBack: "1000000", phone: phone.trim() }],
        reservations: [...prev.reservations, { id: nextReservationId, patientId: nextPatientId, reservedAt, status: "RESERVED", contactName: name.trim(), contactPhone: phone.trim(), memo: "신규 예약" }],
      }));
      return { ok: true, message: "신규 예약 등록 완료" };
    },
    updateReservationEntry: (reservationId, payload) => {
      const target = demo.reservations.find((r) => r.id === reservationId);
      if (!target) return { ok: false, message: "예약 정보를 찾을 수 없습니다." };
      setDemo((prev) => ({
        ...prev,
        reservations: prev.reservations.map((r) => r.id === reservationId ? { ...r, reservedAt: payload.reservedAt, contactName: payload.name.trim(), contactPhone: payload.phone.trim() } : r),
        patients: prev.patients.map((p) => p.id === target.patientId ? { ...p, name: payload.name.trim(), phone: payload.phone.trim() } : p),
      }));
      return { ok: true, message: "예약 수정 완료" };
    },
    addVisit: ({ patientId, visitType }) => {
      const p = demo.patients.find((it) => it.id === patientId);
      if (!p) return { ok: false, message: "환자 정보를 찾을 수 없습니다." };
      const cap = guardedCapacity(1);
      if (!cap.ok) return cap;
      const nextId = Math.max(11000, ...demo.visits.map(v => v.id)) + 1;
      const seq = demo.visits.filter(v => v.registeredAt.slice(0, 10) === new Date().toISOString().slice(0, 10)).length + 1;
      setDemo((prev) => ({
        ...prev,
        visits: [...prev.visits, { id: nextId, patientId, status: "WAITING", registeredAt: now(), queueNo: `A-${String(seq).padStart(3, "0")}`, visitType }],
        reservations: prev.reservations.map((r) => visitType === "RESERVATION" && r.patientId === patientId && r.status === "RESERVED" ? { ...r, status: "CHECKED_IN" } : r),
      }));
      return { ok: true, message: "접수 등록 완료" };
    },
    registerVisitEntry: ({ mode, reservationId, patientName, gender, rrnFront, rrnBack, phone }) => {
      if (!patientName.trim() || !rrnFront.trim() || !rrnBack.trim()) return { ok: false, message: "환자명/주민번호를 입력해주세요." };
      const cap = guardedCapacity(1);
      if (!cap.ok) return cap;
      const targetReservation = reservationId ? demo.reservations.find((r) => r.id === reservationId) : undefined;
      let patientId = targetReservation?.patientId;
      if (!patientId) {
        const existing = demo.patients.find((p) => p.name === patientName.trim() && p.phone === phone.trim());
        patientId = existing?.id;
      }
      const nextPatientId = Math.max(2000, ...demo.patients.map((p) => p.id)) + 1;
      const nextVisitId = Math.max(11000, ...demo.visits.map((v) => v.id)) + 1;
      const seq = demo.visits.filter(v => v.registeredAt.slice(0, 10) === new Date().toISOString().slice(0, 10)).length + 1;
      setDemo((prev) => ({
        ...prev,
        patients: patientId ? prev.patients.map((p) => p.id === patientId ? { ...p, name: patientName.trim(), gender, rrnFront, rrnBack, phone: phone.trim() } : p) : [...prev.patients, { id: nextPatientId, name: patientName.trim(), gender, rrnFront, rrnBack, phone: phone.trim() }],
        visits: [...prev.visits, { id: nextVisitId, patientId: patientId ?? nextPatientId, status: "WAITING", registeredAt: now(), queueNo: `A-${String(seq).padStart(3, "0")}`, visitType: mode, sourceReservationId: reservationId }],
        reservations: prev.reservations.map((r) => reservationId && r.id === reservationId ? { ...r, status: "CHECKED_IN" } : r),
      }));
      return { ok: true, message: mode === "RESERVATION" ? "예약내원 접수 완료" : "현장 접수 등록 완료" };
    },
    updateVisitEntry: (visitId, payload) => {
      const visit = demo.visits.find((v) => v.id === visitId);
      if (!visit) return { ok: false, message: "접수 정보를 찾을 수 없습니다." };
      setDemo((prev) => ({
        ...prev,
        visits: prev.visits.map((v) => v.id === visitId ? { ...v, status: payload.status } : v),
        patients: prev.patients.map((p) => p.id === visit.patientId ? { ...p, name: payload.patientName.trim(), gender: payload.gender, rrnFront: payload.rrnFront, rrnBack: payload.rrnBack, phone: payload.phone.trim() } : p),
      }));
      return { ok: true, message: "접수 수정 완료" };
    },
    removeVisitEntry: (visitId) => {
      setDemo((prev) => ({ ...prev, visits: prev.visits.filter((v) => v.id !== visitId) }));
      return { ok: true, message: "접수 삭제 완료" };
    },
    updateVisitStatus: (visitId, status) => {
      const target = demo.visits.find((v) => v.id === visitId);
      if (!target) return { ok: false, message: "접수 정보를 찾을 수 없습니다." };
      if (!isManualVisitTransitionAllowed(target.status, status)) {
        return { ok: false, message: `허용되지 않은 상태 전이 (${target.status} -> ${status}) · 수동 변경은 대기→진료중→완료만 허용` };
      }
      if (target.status === status) return { ok: true, message: "이미 해당 상태입니다." };
      setDemo((prev) => ({
        ...prev,
        visits: prev.visits.map((v) => (v.id === visitId ? { ...v, status } : v)),
      }));
      return { ok: true, message: "접수 상태 변경 완료" };
    },
    getUnmaskedRrn: (visitId, reason) => {
      const visit = demo.visits.find((v) => v.id === visitId);
      if (!visit) return { ok: false, message: "접수 정보를 찾을 수 없습니다." };
      const patient = demo.patients.find((p) => p.id === visit.patientId);
      if (!patient) return { ok: false, message: "환자 정보를 찾을 수 없습니다." };
      const role = session?.role;
      if (!role || (role !== "ADMIN" && role !== "SYS")) {
        return { ok: false, message: "주민번호 전체보기는 ADMIN/SYS만 가능합니다." };
      }
      setDemo((prev) => ({
        ...prev,
        rrnUnmaskAudit: [
          ...prev.rrnUnmaskAudit,
          {
            visitId,
            patientId: patient.id,
            roleCode: role,
            reason: reason || DEFAULT_UNMASK_REASON,
            createdAt: now(),
          },
        ],
      }));
      return { ok: true, rrn: `${patient.rrnFront}-${patient.rrnBack}`, message: "감사로그 기록 후 주민번호 전체보기 제공" };
    },
    saveSoap: (visitId, data) => {
      setDemo((prev) => ({
        ...prev,
        soaps: {
          ...prev.soaps,
          [visitId]: { visitId, ...data, updatedAt: now() },
        },
      }));
      return { ok: true, message: "SOAP 저장 완료" };
    },
    saveExamOrders: (visitId, items) => {
      setDemo((prev) => ({
        ...prev,
        examOrders: { ...prev.examOrders, [visitId]: items },
      }));
      return { ok: true, message: `검사/영상 오더 ${items.length}건 저장` };
    },
    saveFinalOrder: (draft) => {
      const hasNone = draft.types.includes("NONE");
      const hasOther = draft.types.some((t) => t !== "NONE");
      if (hasNone && hasOther) {
        return { ok: false, message: "이상소견없음(NONE)은 단독 선택만 가능합니다." };
      }
      if (draft.types.includes("ADMISSION") && draft.admission) {
        const nights = calcNights(draft.admission.admitDate, draft.admission.dischargeDate);
        if (nights < 1) return { ok: false, message: "입원 기간은 최소 1박 이상이어야 합니다." };
        draft = { ...draft, admission: { ...draft.admission, nights } };
      }
      setDemo((prev) => {
        const next = {
          ...prev,
          finalOrders: {
            ...prev.finalOrders,
            [draft.visitId]: { ...draft, updatedAt: now() },
          },
          visits: prev.visits.map((v) => {
            if (v.id !== draft.visitId) return v;
            if (hasNone) return { ...v, status: "COMPLETED" as const };
            return v.status === "WAITING" ? { ...v, status: "IN_TREATMENT" as const } : v;
          }),
        };
        return next;
      });
      return { ok: true, message: hasNone ? "NONE 최종오더 저장 + 즉시 완료 처리" : "최종오더 저장 완료" };
    },
    generateInvoiceFromFinalOrder: (visitId) => {
      const finalOrder = demo.finalOrders[visitId];
      if (!finalOrder) return { ok: false, message: "최종오더가 없습니다." };
      const items = buildInvoiceItems(finalOrder);
      const invoiceId = Math.max(9000, ...demo.invoices.map((i) => i.invoiceId)) + 1;
      const newInvoice = {
        invoiceId,
        visitId,
        status: "UNPAID" as const,
        createdAt: now(),
        items,
        totalAmount: totalAmount(items),
      };
      setDemo((prev) => ({
        ...prev,
        invoices: [
          ...prev.invoices.filter((i) => i.visitId !== visitId || i.status === "PAID"),
          newInvoice,
        ],
      }));
      return { ok: true, message: `영수증/청구 생성 완료 (${newInvoice.totalAmount.toLocaleString("ko-KR")}원)` };
    },
    payInvoice: (invoiceId, method) => {
      const invoice = demo.invoices.find((i) => i.invoiceId === invoiceId);
      if (!invoice) return { ok: false, message: "영수증을 찾을 수 없습니다." };
      setDemo((prev) => ({
        ...prev,
        invoices: prev.invoices.map((i) =>
          i.invoiceId === invoiceId ? { ...i, status: "PAID", paymentMethod: method, paidAt: now() } : i
        ),
        visits: prev.visits.map((v) => {
          if (v.id !== invoice.visitId) return v;
          return { ...v, status: "COMPLETED" };
        }),
      }));
      return { ok: true, message: "결제 완료 + 진료완료 상태 반영" };
    },
    // [MODIFIED] Bug #3 수정: 서버 staffId(>0) 는 그대로 유지, 0일 때만 nextId 발급
    upsertStaff: (staff) => {
      setDemo((prev) => {
        const exists = prev.staff.some((s) => s.staffId === staff.staffId);
        if (exists) {
          return { ...prev, staff: prev.staff.map((s) => (s.staffId === staff.staffId ? staff : s)) };
        }
        // staffId > 0 → 서버 발급 ID 그대로 삽입 (nextId 재할당 금지)
        // staffId === 0 → 로컬 데모 신규: nextId 발급
        if (staff.staffId > 0) {
          return { ...prev, staff: [...prev.staff, staff] };
        }
        const nextId = Math.max(0, ...prev.staff.map((s) => s.staffId)) + 1;
        return { ...prev, staff: [...prev.staff, { ...staff, staffId: nextId }] };
      });
      return { ok: true, message: "직원 프로필 저장 완료" };
    },
    removeStaff: (staffId) => {
      setDemo((prev) => ({ ...prev, staff: prev.staff.filter((s) => s.staffId !== staffId) }));
      return { ok: true, message: "직원 프로필 삭제 완료" };
    },
  };

  return <HospitalContext.Provider value={value}>{children}</HospitalContext.Provider>;
}

export function useHospital() {
  const ctx = useContext(HospitalContext);
  if (!ctx) throw new Error("useHospital must be used within HospitalProvider");
  return ctx;
}

export function usePatientVisitRows() {
  const { demo, patientsById } = useHospital();
  return useMemo(
    () =>
      demo.visits
        .slice()
        .sort((a, b) => b.id - a.id)
        .map((v) => ({ visit: v, patient: patientsById[v.patientId] })),
    [demo.visits, patientsById]
  );
}

export const EXAM_OPTIONS: Record<ExamCategory, ExamOrderItem[]> = {
  LAB: [
    { category: "LAB", code: "LAB_BLOOD", name: "혈액검사" },
    { category: "LAB", code: "LAB_URINE", name: "소변검사" },
    { category: "LAB", code: "LAB_NONE", name: "검사없음" },
  ],
  RAD: [
    { category: "RAD", code: "RAD_MRI", name: "MRI" },
    { category: "RAD", code: "RAD_CT", name: "CT" },
    { category: "RAD", code: "RAD_US", name: "초음파검사" },
    { category: "RAD", code: "RAD_NONE", name: "검사없음" },
  ],
  PROC: [
    { category: "PROC", code: "PROC_GASTRO", name: "위내시경" },
    { category: "PROC", code: "PROC_COLON", name: "대장내시경" },
    { category: "PROC", code: "PROC_NONE", name: "검사없음" },
  ],
};

export function normalizeExamSelection(items: ExamOrderItem[]): ExamOrderItem[] {
  const byCategory = new Map<ExamCategory, ExamOrderItem[]>();
  for (const item of items) {
    const arr = byCategory.get(item.category) ?? [];
    arr.push(item);
    byCategory.set(item.category, arr);
  }
  const normalized: ExamOrderItem[] = [];
  (["LAB", "RAD", "PROC"] as ExamCategory[]).forEach((cat) => {
    const selected = byCategory.get(cat) ?? [];
    const hasNone = selected.some((i) => i.code.endsWith("_NONE"));
    if (hasNone) {
      const none = selected.find((i) => i.code.endsWith("_NONE"));
      if (none) normalized.push(none);
      return;
    }
    normalized.push(...selected);
  });
  return normalized;
}

export function computeAdmission(payload: { wardNo: number; admitDate: string; dischargeDate: string }): FinalOrderAdmission | undefined {
  const nights = calcNights(payload.admitDate, payload.dischargeDate);
  if (nights < 1) return undefined;
  return { ...payload, nights };
}
