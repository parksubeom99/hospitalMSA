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
  ExamCategory,
  ExamOrderItem,
  FinalOrderAdmission,
  FinalOrderDraft,
  HospitalState,
  MedicationCatalogItem,
  RoleCode,
  StaffProfile,
  UserSession,
  VisitStatus,
} from "@/shared/types/domain";

const STORAGE_KEY = "hospital-msa-front-4svc-v2";

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

function createSeedState(): HospitalState {
  // [MODIFIED] new Date() → 고정 날짜 문자열
  // 이유: SSR(서버) 실행 시각 ≠ CSR(클라이언트) hydration 시각 → React #425/#418/#423 hydration mismatch
  // 해결: seed 데이터는 고정 ISO 문자열 사용, 런타임 now()는 액션 시점에만 호출
  const SEED_DATE = "2026-01-15";
  const at = (h: number, m: number) => {
    const hh = String(h).padStart(2, "0");
    const mm = String(m).padStart(2, "0");
    return `${SEED_DATE}T${hh}:${mm}:00.000Z`;
  };
  return {
    session: null,
    emergencyCount: 3,
    patients: [
      { id: 2001, name: "박서준", gender: "M", rrnFront: "982223", rrnBack: "1234567", phone: "010-8762-1111" },
      { id: 2002, name: "이지은", gender: "F", rrnFront: "990101", rrnBack: "2345678", phone: "010-3456-2222" },
      { id: 2003, name: "김민수", gender: "M", rrnFront: "010321", rrnBack: "3456789", phone: "010-8888-3333" },
      { id: 2004, name: "박민재", gender: "M", rrnFront: "030412", rrnBack: "4123456", phone: "010-2222-4444" },
      { id: 2005, name: "최수진", gender: "F", rrnFront: "950722", rrnBack: "2456123", phone: "010-3333-5555" },
      { id: 2006, name: "정수현", gender: "M", rrnFront: "900101", rrnBack: "1987654", phone: "010-4444-6666" },
      { id: 2007, name: "한가인", gender: "F", rrnFront: "920318", rrnBack: "2123456", phone: "010-1234-7777" },
      { id: 2008, name: "유재석", gender: "M", rrnFront: "760814", rrnBack: "1234567", phone: "010-5555-8888" },
    ],
    reservations: [
      { id: 501, patientId: 2001, reservedAt: at(14, 30), status: "RESERVED", memo: "초진 예약", contactName: "박서준", contactPhone: "010-8762-1111" },
      { id: 502, patientId: 2002, reservedAt: at(15, 0), status: "RESERVED", memo: "재진 예약", contactName: "이지은", contactPhone: "010-3456-2222" },
      { id: 503, patientId: 2005, reservedAt: at(16, 20), status: "RESERVED", memo: "복약 상담", contactName: "최수진", contactPhone: "010-3333-5555" },
    ],
    visits: [
      { id: 11001, patientId: 2003, status: "WAITING", registeredAt: at(9, 12), queueNo: "A-001", visitType: "WALK_IN" },
      { id: 11002, patientId: 2004, status: "IN_TREATMENT", registeredAt: at(9, 18), queueNo: "A-002", visitType: "WALK_IN" },
      { id: 11003, patientId: 2006, status: "WAITING", registeredAt: at(9, 24), queueNo: "A-003", visitType: "WALK_IN" },
      { id: 11004, patientId: 2007, status: "COMPLETED", registeredAt: at(8, 50), queueNo: "A-004", visitType: "RESERVATION" },
    ],
    soaps: {
      11002: {
        visitId: 11002,
        subjective: "복통 호소",
        objective: "복부 압통 경미",
        assessment: "급성 위염 의심",
        plan: "검사 후 약 처방 검토",
        updatedAt: at(10, 20),
      },
    },
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
  // [ADDED] hydrated: localStorage 복원 완료 여부
  // SSR(false) → client useEffect 완료(true)
  // RoleGate가 이 값을 보고 hydration 전 렌더를 억제 → #418/#423/#425 해소
  hydrated: boolean;
  state: HospitalState;
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
  patientsById: Record<number, HospitalState["patients"][number]>;
}

const HospitalContext = createContext<HospitalContextValue | null>(null);

function buildCapacity(state: HospitalState): CapacitySummary {
  const waitingAndInTreatment = state.visits.filter(v => !v.cancelled && (v.status === "WAITING" || v.status === "IN_TREATMENT")).length;
  const reservation = state.reservations.filter(r => r.status === "RESERVED").length;
  const emergency = state.emergencyCount;
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
  const [state, setState] = useState<HospitalState>(createSeedState);
  // [ADDED] hydrated: localStorage 복원 완료 플래그
  // 초기값 false → 첫 번째 useEffect(localStorage 복원) 완료 후 true
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw) as HospitalState;
      if (parsed && parsed.patients && parsed.visits) {
        const normalizedSessionRole = normalizeRoleCode((parsed as any)?.session?.role);
        const normalized = {
          ...parsed,
          session: parsed.session
            ? {
                ...parsed.session,
                role: (normalizedSessionRole ?? "SYS") as RoleCode,
                authSource: parsed.session.authSource === "server" ? "server" : "demo",
              }
            : null,
          rrnUnmaskAudit: (parsed.rrnUnmaskAudit ?? []).map((a) => ({
            ...a,
            roleCode: (normalizeRoleCode((a as any).roleCode) ?? "SYS") as RoleCode,
          })),
        } as HospitalState;
        setState(normalized);
      }
    } catch {
      // ignore malformed local state
    } finally {
      // [ADDED] localStorage 복원 시도 완료 (성공/실패 무관) → hydrated=true
      // 이 시점부터 RoleGate가 실제 session 기반으로 렌더링 허용
      setHydrated(true);
    }
  }, []);

  useEffect(() => {
    try {
      const persistable = state.session
        ? { ...state, session: { ...state.session, accessToken: undefined } }
        : state;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(persistable));
    } catch {
      // storage may be unavailable
    }
  }, [state]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const tokens = loadStoredAuthTokens();
    if (!tokens?.accessToken) return;
    setState((prev) => {
      if (!prev.session) return prev;
      return {
        ...prev,
        session: {
          ...prev.session,
          accessToken: tokens.accessToken,
          tokenType: "Bearer",
        },
      };
    });
  }, []);

  const capacity = useMemo(() => buildCapacity(state), [state]);

  const patientsById = useMemo(() => {
    return Object.fromEntries(state.patients.map((p) => [p.id, p]));
  }, [state.patients]);

  const guardedCapacity = (delta: number): ActionResult => {
    const next = capacity.current + delta;
    if (next > MAX_TOTAL_CAPACITY) return { ok: false, message: `총 운영 인원 30명 제한 초과 (현재 ${capacity.current}명)` };
    if (next < 0) return { ok: false, message: "인원 수가 0 미만이 될 수 없습니다." };
    return { ok: true, message: "OK" };
  };

  const value: HospitalContextValue = {
    hydrated, // [ADDED]
    state,
    capacity,
    medicationCatalog: MEDICATION_CATALOG,
    patientsById,
    loginAs: (role) => {
      const displayName = role === "DOC" ? "의사 계정" : role === "ADMIN" ? "원무 계정" : "시스템관리자";
      const token = makeMockJwt({ sub: role, role, name: displayName, iat: Date.now() });
      saveStoredAuthTokens({ accessToken: token, tokenType: "Bearer" });
      setState((prev) => ({ ...prev, session: { role, displayName, username: role.toLowerCase(), accessToken: token, tokenType: "Bearer", authSource: "demo" } }));
    },
    loginWithCredentials: ({ username, password }) => {
      const account = MOCK_ACCOUNTS.find((a) => a.username === username && a.password === password);
      if (!account) return { ok: false, message: "아이디 또는 비밀번호가 올바르지 않습니다." };
      const token = makeMockJwt({ sub: account.username, role: account.role, name: account.displayName, iat: Date.now() });
      saveStoredAuthTokens({ accessToken: token, tokenType: "Bearer" });
      setState((prev) => ({
        ...prev,
        session: {
          role: account.role,
          displayName: account.displayName,
          username: account.username,
          accessToken: token,
          tokenType: "Bearer",
          authSource: "demo",
          doctorStaffId: (account as any).doctorStaffId,
        },
      }));
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
        setState((prev) => ({
          ...prev,
          session: {
            role: me.role,
            displayName: me.displayName,
            username: me.username || result.user.username,
            accessToken: result.token.accessToken,
            tokenType: result.token.tokenType,
            authSource: "server",
            doctorStaffId: me.staffId,
          },
        }));
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
        setState((prev) => ({
          ...prev,
          session: {
            role: me.role,
            displayName: me.displayName,
            username: me.username,
            accessToken: tokens.accessToken,
            tokenType: "Bearer",
            authSource: "server",
            doctorStaffId: me.staffId,
          },
        }));
        return { ok: true, message: "저장된 로그인 세션 복원 완료" };
      } catch (e) {
        clearStoredAuthTokens();
        setState((prev) => ({ ...prev, session: null }));
        return { ok: false, message: e instanceof Error ? e.message : "세션 복원 실패" };
      }
    },
    logout: () => { void logoutAuth().catch(() => clearStoredAuthTokens()); setState((prev) => ({ ...prev, session: null })); },
    resetDemoData: () => setState(createSeedState()),
    setEmergencyCount: (value) => {
      if (!Number.isInteger(value) || value < 0 || value > MAX_EMERGENCY_COUNT) {
        return { ok: false, message: "응급 환자 수는 0~10 범위여야 합니다." };
      }
      const delta = value - state.emergencyCount;
      const cap = guardedCapacity(delta);
      if (!cap.ok) return cap;
      setState((prev) => ({ ...prev, emergencyCount: value }));
      return { ok: true, message: `응급 카운터를 ${value}명으로 변경했습니다.` };
    },
    addReservation: ({ patientId, reservedAt, memo }) => {
      const patient = state.patients.find((p) => p.id === patientId);
      const cap = guardedCapacity(1);
      if (!cap.ok) return cap;
      const nextId = Math.max(500, ...state.reservations.map(r => r.id)) + 1;
      setState((prev) => ({
        ...prev,
        reservations: [...prev.reservations, { id: nextId, patientId, reservedAt, status: "RESERVED", memo, contactName: patient?.name, contactPhone: patient?.phone }],
      }));
      return { ok: true, message: "예약 등록 완료" };
    },
    createReservationEntry: ({ name, phone, reservedAt }) => {
      const cap = guardedCapacity(1);
      if (!cap.ok) return cap;
      if (!name.trim() || !phone.trim() || !reservedAt) return { ok: false, message: "이름/전화번호/예약시간대를 입력해주세요." };
      const nextPatientId = Math.max(2000, ...state.patients.map((p) => p.id)) + 1;
      const nextReservationId = Math.max(500, ...state.reservations.map((r) => r.id)) + 1;
      setState((prev) => ({
        ...prev,
        patients: [...prev.patients, { id: nextPatientId, name: name.trim(), gender: "M", rrnFront: "000000", rrnBack: "1000000", phone: phone.trim() }],
        reservations: [...prev.reservations, { id: nextReservationId, patientId: nextPatientId, reservedAt, status: "RESERVED", contactName: name.trim(), contactPhone: phone.trim(), memo: "신규 예약" }],
      }));
      return { ok: true, message: "신규 예약 등록 완료" };
    },
    updateReservationEntry: (reservationId, payload) => {
      const target = state.reservations.find((r) => r.id === reservationId);
      if (!target) return { ok: false, message: "예약 정보를 찾을 수 없습니다." };
      setState((prev) => ({
        ...prev,
        reservations: prev.reservations.map((r) => r.id === reservationId ? { ...r, reservedAt: payload.reservedAt, contactName: payload.name.trim(), contactPhone: payload.phone.trim() } : r),
        patients: prev.patients.map((p) => p.id === target.patientId ? { ...p, name: payload.name.trim(), phone: payload.phone.trim() } : p),
      }));
      return { ok: true, message: "예약 수정 완료" };
    },
    addVisit: ({ patientId, visitType }) => {
      const p = state.patients.find((it) => it.id === patientId);
      if (!p) return { ok: false, message: "환자 정보를 찾을 수 없습니다." };
      const cap = guardedCapacity(1);
      if (!cap.ok) return cap;
      const nextId = Math.max(11000, ...state.visits.map(v => v.id)) + 1;
      const seq = state.visits.filter(v => v.registeredAt.slice(0, 10) === new Date().toISOString().slice(0, 10)).length + 1;
      setState((prev) => ({
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
      const targetReservation = reservationId ? state.reservations.find((r) => r.id === reservationId) : undefined;
      let patientId = targetReservation?.patientId;
      if (!patientId) {
        const existing = state.patients.find((p) => p.name === patientName.trim() && p.phone === phone.trim());
        patientId = existing?.id;
      }
      const nextPatientId = Math.max(2000, ...state.patients.map((p) => p.id)) + 1;
      const nextVisitId = Math.max(11000, ...state.visits.map((v) => v.id)) + 1;
      const seq = state.visits.filter(v => v.registeredAt.slice(0, 10) === new Date().toISOString().slice(0, 10)).length + 1;
      setState((prev) => ({
        ...prev,
        patients: patientId ? prev.patients.map((p) => p.id === patientId ? { ...p, name: patientName.trim(), gender, rrnFront, rrnBack, phone: phone.trim() } : p) : [...prev.patients, { id: nextPatientId, name: patientName.trim(), gender, rrnFront, rrnBack, phone: phone.trim() }],
        visits: [...prev.visits, { id: nextVisitId, patientId: patientId ?? nextPatientId, status: "WAITING", registeredAt: now(), queueNo: `A-${String(seq).padStart(3, "0")}`, visitType: mode, sourceReservationId: reservationId }],
        reservations: prev.reservations.map((r) => reservationId && r.id === reservationId ? { ...r, status: "CHECKED_IN" } : r),
      }));
      return { ok: true, message: mode === "RESERVATION" ? "예약내원 접수 완료" : "현장 접수 등록 완료" };
    },
    updateVisitEntry: (visitId, payload) => {
      const visit = state.visits.find((v) => v.id === visitId);
      if (!visit) return { ok: false, message: "접수 정보를 찾을 수 없습니다." };
      setState((prev) => ({
        ...prev,
        visits: prev.visits.map((v) => v.id === visitId ? { ...v, status: payload.status } : v),
        patients: prev.patients.map((p) => p.id === visit.patientId ? { ...p, name: payload.patientName.trim(), gender: payload.gender, rrnFront: payload.rrnFront, rrnBack: payload.rrnBack, phone: payload.phone.trim() } : p),
      }));
      return { ok: true, message: "접수 수정 완료" };
    },
    removeVisitEntry: (visitId) => {
      setState((prev) => ({ ...prev, visits: prev.visits.filter((v) => v.id !== visitId) }));
      return { ok: true, message: "접수 삭제 완료" };
    },
    updateVisitStatus: (visitId, status) => {
      const target = state.visits.find((v) => v.id === visitId);
      if (!target) return { ok: false, message: "접수 정보를 찾을 수 없습니다." };
      if (!isManualVisitTransitionAllowed(target.status, status)) {
        return { ok: false, message: `허용되지 않은 상태 전이 (${target.status} -> ${status}) · 수동 변경은 대기→진료중→완료만 허용` };
      }
      if (target.status === status) return { ok: true, message: "이미 해당 상태입니다." };
      setState((prev) => ({
        ...prev,
        visits: prev.visits.map((v) => (v.id === visitId ? { ...v, status } : v)),
      }));
      return { ok: true, message: "접수 상태 변경 완료" };
    },
    getUnmaskedRrn: (visitId, reason) => {
      const visit = state.visits.find((v) => v.id === visitId);
      if (!visit) return { ok: false, message: "접수 정보를 찾을 수 없습니다." };
      const patient = state.patients.find((p) => p.id === visit.patientId);
      if (!patient) return { ok: false, message: "환자 정보를 찾을 수 없습니다." };
      const role = state.session?.role;
      if (!role || (role !== "ADMIN" && role !== "SYS")) {
        return { ok: false, message: "주민번호 전체보기는 ADMIN/SYS만 가능합니다." };
      }
      setState((prev) => ({
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
      setState((prev) => ({
        ...prev,
        soaps: {
          ...prev.soaps,
          [visitId]: { visitId, ...data, updatedAt: now() },
        },
      }));
      return { ok: true, message: "SOAP 저장 완료" };
    },
    saveExamOrders: (visitId, items) => {
      setState((prev) => ({
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
      setState((prev) => {
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
      const finalOrder = state.finalOrders[visitId];
      if (!finalOrder) return { ok: false, message: "최종오더가 없습니다." };
      const items = buildInvoiceItems(finalOrder);
      const invoiceId = Math.max(9000, ...state.invoices.map((i) => i.invoiceId)) + 1;
      const newInvoice = {
        invoiceId,
        visitId,
        status: "UNPAID" as const,
        createdAt: now(),
        items,
        totalAmount: totalAmount(items),
      };
      setState((prev) => ({
        ...prev,
        invoices: [
          ...prev.invoices.filter((i) => i.visitId !== visitId || i.status === "PAID"),
          newInvoice,
        ],
      }));
      return { ok: true, message: `영수증/청구 생성 완료 (${newInvoice.totalAmount.toLocaleString("ko-KR")}원)` };
    },
    payInvoice: (invoiceId, method) => {
      const invoice = state.invoices.find((i) => i.invoiceId === invoiceId);
      if (!invoice) return { ok: false, message: "영수증을 찾을 수 없습니다." };
      setState((prev) => ({
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
      setState((prev) => {
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
      setState((prev) => ({ ...prev, staff: prev.staff.filter((s) => s.staffId !== staffId) }));
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
  const { state, patientsById } = useHospital();
  return useMemo(
    () =>
      state.visits
        .slice()
        .sort((a, b) => b.id - a.id)
        .map((v) => ({ visit: v, patient: patientsById[v.patientId] })),
    [state.visits, patientsById]
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
