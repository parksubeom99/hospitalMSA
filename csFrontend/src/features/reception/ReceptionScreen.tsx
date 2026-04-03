"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { GlassCard } from "@/shared/components/GlassCard";
import { RoleGate } from "@/shared/components/RoleGate";
import { StatusBadge } from "@/shared/components/StatusBadge";
import { useHospital } from "@/shared/store/HospitalStore";
import { formatDateTime, getTodayKST, nowDateTimeRounded } from "@/shared/lib/date"; // [MODIFIED] getTodayKST 추가
import { formatRrnMasked, maskName, maskPhone } from "@/shared/lib/masking";
import { STATUS_LABEL } from "@/shared/config/constants";
import type { VisitStatus } from "@/shared/types/domain";
import { cancelVisitServer, checkInReservationServer, createReservationServer, createVisitServer, updateVisitServer, upsertPatientForReception } from "@/shared/services/receptionMutationApi";
import { updateEmergencyCount } from "@/shared/services/dashboard/dashboardApi";
import { useReservationsQuery, useVisitsQuery, receptionQueryKeys } from "@/shared/services/reception/receptionQueries";
import type { SyncedReservationRow } from "@/shared/services/reception/receptionApi";

type ReceptionTab = "RESERVATION" | "WAITING" | "EMERGENCY";

type VisitForm = {
  mode: "WALK_IN" | "RESERVATION";
  reservationId?: number;
  patientName: string;
  gender: "M" | "F";
  rrnFront: string;
  rrnBack: string;
  phone: string;
  status: VisitStatus;
};

export function ReceptionScreen() {
  const {
    state,
    capacity,
    patientsById,
    createReservationEntry,
    updateReservationEntry,
    registerVisitEntry,
    updateVisitEntry,
    removeVisitEntry,
    setEmergencyCount,
  } = useHospital() as any;

  const qc = useQueryClient();

  const serverWriteEnabled = state.session?.authSource === "server";

  // [MODIFIED] UTC → KST 기준 날짜 (버그 #3)
  const today = getTodayKST();
  const reservationsQuery = useReservationsQuery({ date: today });
  const visitsQuery = useVisitsQuery();

  const syncLoading = reservationsQuery.isFetching || visitsQuery.isFetching;

  const [tab, setTab] = useState<ReceptionTab>("RESERVATION");
  const [toast, setToast] = useState("");

  const [reservationForm, setReservationForm] = useState({ name: "", phone: "", reservedAt: nowDateTimeRounded() });
  const [editingReservationId, setEditingReservationId] = useState<number | null>(null);

  // ── [MODIFIED] activeReservations — 서버 데이터 우선, 로컬 fallback (버그 #2) ──────────
  // 이유: reservationsQuery.refetch() 해도 state.reservations(로컬)만 보면 서버 데이터 무시됨
  // 서버 세션 + 서버 데이터(BOOKED/RESERVED 상태) 있으면 서버 데이터 우선 사용
  const isServerReservation = serverWriteEnabled
    && Array.isArray(reservationsQuery.data)
    && reservationsQuery.data.length > 0;

  const activeReservations = useMemo(() => {
    if (isServerReservation) {
      // 서버: CHECKED_IN/CANCELLED 제외, RESERVED(=BOOKED)만 표시
      return [...(reservationsQuery.data as SyncedReservationRow[])]
        .filter((r) => r.status === "RESERVED")
        .sort((a, b) => a.reservedAt.localeCompare(b.reservedAt));
    }
    // 로컬 fallback
    return state.reservations
      .filter((r: any) => r.status === "RESERVED")
      .sort((a: any, b: any) => a.reservedAt.localeCompare(b.reservedAt));
  }, [isServerReservation, reservationsQuery.data, state.reservations]);
  // ────────────────────────────────────────────────────────────────────────────────────────

  // ── 접수 목록 — 서버 데이터 우선, 로컬 fallback ──────────────────────────────────────────
  const isServerData = serverWriteEnabled && Array.isArray(visitsQuery.data) && visitsQuery.data.length > 0;
  const receptionRows = useMemo(() => {
    if (isServerData) {
      return [...visitsQuery.data!].sort((a: any, b: any) => b.id - a.id);
    }
    return state.visits.slice().sort((a: any, b: any) => b.id - a.id);
  }, [isServerData, visitsQuery.data, state.visits]);
  // ────────────────────────────────────────────────────────────────────────────────────────

  const [visitForm, setVisitForm] = useState<VisitForm>({
    mode: "WALK_IN",
    patientName: "",
    gender: "M",
    rrnFront: "",
    rrnBack: "",
    phone: "",
    status: "WAITING",
  });
  const [editingVisitId, setEditingVisitId] = useState<number | null>(null);
  const [rrnVisible, setRrnVisible] = useState(false);
  const [emergencyValue, setEmergencyValue] = useState<number>(state.emergencyCount);
  const reservationPhoneMidRef = useRef<HTMLInputElement | null>(null);
  const reservationPhoneLastRef = useRef<HTMLInputElement | null>(null);
  const visitPhoneMidRef = useRef<HTMLInputElement | null>(null);
  const visitPhoneLastRef = useRef<HTMLInputElement | null>(null);

  const digitsOnly = (v: string) => v.replace(/\D/g, "");
  const splitPhone = (phone: string) => {
    const d = digitsOnly(phone);
    const body = d.startsWith("010") ? d.slice(3) : d;
    return { mid: body.slice(0, 4), last: body.slice(4, 8) };
  };
  const joinPhone = (mid: string, last: string) => `010-${digitsOnly(mid).slice(0,4)}-${digitsOnly(last).slice(0,4)}`;

  const showToast = (message: string) => {
    setToast(message);
    window.setTimeout(() => setToast(""), 2200);
  };

  useEffect(() => {
    setEmergencyValue(state.emergencyCount);
  }, [state.emergencyCount]);

  const resetReservationForm = () => {
    setEditingReservationId(null);
    setReservationForm({ name: "", phone: "", reservedAt: nowDateTimeRounded() });
  };

  const resetVisitForm = () => {
    setEditingVisitId(null);
    setRrnVisible(false);
    setVisitForm({ mode: "WALK_IN", patientName: "", gender: "M", rrnFront: "", rrnBack: "", phone: "", status: "WAITING" });
  };

  const handleSync = async () => {
    try {
      await Promise.all([
        reservationsQuery.refetch(),
        visitsQuery.refetch(),
      ]);
      showToast("서버 데이터 동기화 완료");
    } catch {
      showToast("동기화 실패");
    }
  };

  const handleReservationSave = async () => {
    const iso = new Date(reservationForm.reservedAt).toISOString();
    try {
      if (serverWriteEnabled && !editingReservationId) {
        // [FIXED P2] slice(-10) → Date.now() 전체 사용 (Long 범위 안전, 400 오류 차단)
        const tempPatientId = Date.now();
        await upsertPatientForReception({
          session: state.session ?? undefined,
          patientId: tempPatientId,
          name: reservationForm.name,
          gender: "M",
          rrnFront: "000000",
          rrnBack: "1000000",
          phone: reservationForm.phone,
        });
        await createReservationServer({
          session: state.session ?? undefined,
          patientId: tempPatientId,
          patientName: reservationForm.name,
          reservedAtIso: iso,
        });
        qc.invalidateQueries({ queryKey: ["reception", "reservations"] });
      }
      const result = editingReservationId
        ? updateReservationEntry(editingReservationId, { ...reservationForm, reservedAt: iso })
        : createReservationEntry({ ...reservationForm, reservedAt: iso });
      showToast(result.message + (serverWriteEnabled && !editingReservationId ? " (서버 저장 포함)" : ""));
      if (result.ok) resetReservationForm();
    } catch (e: any) {
      showToast(`예약 서버 저장 실패: ${e?.message || e}`);
    }
  };

  // [MODIFIED] 예약내원 접수 — 버그 #4/#5 통합 수정
  // 버그 #4: 서버 예약 클릭 시 r.patientId → patientsById에 없을 수 있음
  //          → contactName/contactPhone fallback으로 방문 생성
  // 버그 #5: checkIn 성공 후 로컬 state.reservations status 미동기화
  //          → isServerReservation=true 시 refetch()로 서버 데이터 재조회하여 자동 해결
  const handleRegisterReservationVisit = async (reservationId: number) => {
    // 서버 예약 데이터 우선, 없으면 로컬 fallback
    const serverR = isServerReservation
      ? (reservationsQuery.data as SyncedReservationRow[]).find((it) => it.id === reservationId)
      : undefined;
    const localR = state.reservations.find((it: any) => it.id === reservationId);
    const r = localR ?? serverR; // 로컬에 있으면 patientId 활용, 없으면 서버 데이터
    if (!r) { showToast("예약 정보를 찾을 수 없습니다."); return; }

    // 환자 정보: 로컬 patientsById 우선, 없으면 서버 contactName 사용 (버그 #4)
    const p = patientsById[r.patientId];
    const patientName = p?.name ?? serverR?.contactName ?? r.contactName ?? "";
    const gender: "M" | "F" = p?.gender ?? "M";
    const rrnFront = p?.rrnFront ?? "000000";
    const rrnBack = p?.rrnBack ?? "1000000";
    const phone = p?.phone ?? serverR?.contactPhone ?? r.contactPhone ?? "";

    try {
      if (serverWriteEnabled) {
        await checkInReservationServer({ session: state.session ?? undefined, reservationId });
        // [MODIFIED] 버그 #5: refetch()로 서버 최신 상태 반영 → CHECKED_IN이 activeReservations에서 사라짐
        await reservationsQuery.refetch();
        qc.invalidateQueries({ queryKey: ["reception", "visits"] });
      }
      const result = registerVisitEntry({
        mode: "RESERVATION",
        reservationId,
        patientName,
        gender,
        rrnFront,
        rrnBack,
        phone,
      });
      showToast(result.message + (serverWriteEnabled ? " (서버 체크인 포함)" : ""));
    } catch (e: any) {
      showToast(`예약내원 서버 체크인 실패: ${e?.message || e}`);
    }
  };

  const handleVisitSave = async () => {
    try {
      if (serverWriteEnabled) {
        if (editingVisitId) {
          await updateVisitServer({ session: state.session ?? undefined, visitId: editingVisitId, patientName: visitForm.patientName });
        } else {
          // [FIXED P2] slice(-10) → Date.now() 전체 사용 (Long 범위 안전, 400 오류 차단)
        const tempPatientId = Date.now();
          await upsertPatientForReception({
            session: state.session ?? undefined,
            patientId: tempPatientId,
            name: visitForm.patientName,
            gender: visitForm.gender,
            rrnFront: visitForm.rrnFront,
            rrnBack: visitForm.rrnBack,
            phone: visitForm.phone,
          });
          await createVisitServer({
            session: state.session ?? undefined,
            patientId: tempPatientId,
            patientName: visitForm.patientName,
            mode: visitForm.mode,
          });
        }
        qc.invalidateQueries({ queryKey: ["reception", "visits"] });
      }
      const result = editingVisitId
        ? updateVisitEntry(editingVisitId, {
            patientName: visitForm.patientName,
            gender: visitForm.gender,
            rrnFront: visitForm.rrnFront,
            rrnBack: visitForm.rrnBack,
            phone: visitForm.phone,
            status: visitForm.status,
          })
        : registerVisitEntry({
            mode: visitForm.mode,
            reservationId: visitForm.mode === "RESERVATION" ? visitForm.reservationId : undefined,
            patientName: visitForm.patientName,
            gender: visitForm.gender,
            rrnFront: visitForm.rrnFront,
            rrnBack: visitForm.rrnBack,
            phone: visitForm.phone,
          });
      showToast(result.message + (serverWriteEnabled ? " (서버 저장 포함)" : ""));
      if (result.ok) resetVisitForm();
    } catch (e: any) {
      showToast(`접수 서버 저장 실패: ${e?.message || e}`);
    }
  };

  const selectReservationForEdit = (r: any) => {
    const p = patientsById[r.patientId];
    setEditingReservationId(r.id);
    setReservationForm({ name: p?.name ?? r.contactName ?? "", phone: p?.phone ?? r.contactPhone ?? "", reservedAt: r.reservedAt.slice(0, 16) });
  };

  const selectVisitForEdit = (visit: any) => {
    const p = patientsById[visit.patientId];
    // [FIXED B1] isServerData=true 시 p가 없어도 서버 필드로 폼 채움
    // 이전: p 없으면 return → 수정 버튼 눌러도 폼 변화 없음
    // 수정: visit.patientName(서버) fallback 사용, rrnFront/Back/phone은 빈값 허용
    setEditingVisitId(visit.id);
    setRrnVisible(false);
    setVisitForm({
      mode: visit.visitType,
      reservationId: visit.sourceReservationId,
      patientName: p?.name ?? visit.patientName ?? "",
      gender: p?.gender ?? (visit.gender === "F" ? "F" : "M"),
      rrnFront: p?.rrnFront ?? "",
      rrnBack: p?.rrnBack ?? "",
      phone: p?.phone ?? "",
      status: visit.status,
    });
    setTab("WAITING");
  };

  const reservationPhoneParts = splitPhone(reservationForm.phone);
  const visitPhoneParts = splitPhone(visitForm.phone);

  return (
    <RoleGate allowed={["ADMIN", "SYS"]}>
      <div className="page-grid page-grid--readable">
        <GlassCard
          title="접수"
          subtitle="원무/시스템관리자 전용 · 예약 / 대기 / 응급"
          right={<StatusBadge label={`총 인원 ${capacity.current}/30`} tone={capacity.level === "SAFE" ? "green" : capacity.level === "WARN" ? "orange" : "red"} />}
        >
          <div className="inline-check-group" style={{ marginBottom: 8, display: "flex", alignItems: "center", gap: 12 }}>
            {serverWriteEnabled
              ? <small className="muted">실서버 저장 모드 활성 (IAM 로그인됨)</small>
              : <small className="muted">데모 모드 (실서버 저장 비활성)</small>
            }
            <button type="button" onClick={handleSync} disabled={syncLoading} style={{ marginLeft: "auto" }}>
              {syncLoading ? "동기화 중..." : "동기화"}
            </button>
          </div>

          <div className="tab-row">
            {[{ key: "RESERVATION", label: "예약" }, { key: "WAITING", label: "대기" }, { key: "EMERGENCY", label: "응급" }].map((t) => (
              <button key={t.key} className={`tab-btn ${tab === t.key ? "is-active" : ""}`} onClick={() => setTab(t.key as ReceptionTab)} type="button">{t.label}</button>
            ))}
          </div>

          {tab === "RESERVATION" && (
            <div className="split-grid">
              <GlassCard title={editingReservationId ? "예약 수정" : "예약 등록"} subtitle="신규 환자 예약 (이름 / 전화번호 / 예약시간대)" className="nested-card">
                <div className="form-grid tri">
                  <label><span>이름</span><input value={reservationForm.name} onChange={(e) => setReservationForm((s) => ({ ...s, name: e.target.value }))} /></label>
                  <label><span>전화번호</span><div className="phone-split"><input value="010" readOnly /><input ref={reservationPhoneMidRef} inputMode="numeric" maxLength={4} value={reservationPhoneParts.mid} onChange={(e) => { const v = e.target.value; setReservationForm((s) => ({ ...s, phone: joinPhone(v, splitPhone(s.phone).last) })); if (digitsOnly(v).length >= 4) reservationPhoneLastRef.current?.focus(); }} placeholder="1234" /><input ref={reservationPhoneLastRef} inputMode="numeric" maxLength={4} value={reservationPhoneParts.last} onChange={(e) => setReservationForm((s) => ({ ...s, phone: joinPhone(splitPhone(s.phone).mid, e.target.value) }))} placeholder="5678" /></div></label>
                  <label><span>예약시간대</span><input type="datetime-local" value={reservationForm.reservedAt} min={nowDateTimeRounded().slice(0, 16)} onChange={(e) => setReservationForm((s) => ({ ...s, reservedAt: e.target.value }))} /></label>
                </div>
                <div className="button-row">
                  <button type="button" className="primary-btn" onClick={handleReservationSave} disabled={!capacity.canRegister && !editingReservationId}>{editingReservationId ? "예약 수정" : "예약 등록"}</button>
                  {editingReservationId && <button type="button" onClick={resetReservationForm}>신규 모드</button>}
                </div>
              </GlassCard>

              <GlassCard
                title="예약 현황"
                subtitle="이름/전화번호 마스킹 · 수정/예약내원접수"
                className="nested-card"
                right={
                  <button type="button" onClick={() => reservationsQuery.refetch()} disabled={reservationsQuery.isFetching}>
                    {reservationsQuery.isFetching ? "조회 중..." : "예약 조회"}
                  </button>
                }
              >
                <div className="table-wrap">
                  <table className="ui-table compact">
                    <thead><tr><th>예약번호</th><th>예약시각</th><th>예약자</th><th>전화번호</th><th>상태</th><th>관리</th></tr></thead>
                    <tbody>
                      {activeReservations.map((r: any) => {
                        const p = patientsById[r.patientId];
                        const nm = p?.name ?? r.contactName ?? "-";
                        const ph = p?.phone ?? r.contactPhone ?? "-";
                        return (
                          <tr key={r.id}>
                            <td>{r.id}</td>
                            <td>{formatDateTime(r.reservedAt)}</td>
                            <td>{maskName(nm)}</td>
                            <td>{maskPhone(ph)}</td>
                            <td>예약</td>
                            <td>
                              <div className="inline-btns">
                                <button type="button" onClick={() => selectReservationForEdit(r)}>수정</button>
                                <button type="button" onClick={() => handleRegisterReservationVisit(r.id)}>예약내원 접수</button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </GlassCard>
            </div>
          )}

          {tab === "WAITING" && (
            <div className="stack-col">
              <GlassCard title={editingVisitId ? "접수 수정" : "접수 등록"} subtitle="예약내원 또는 현장 방문 접수 / 접수번호 자동생성" className="nested-card">
                <div className="form-grid tri">
                  <label>
                    <span>접수 유형</span>
                    <select value={visitForm.mode} onChange={(e) => setVisitForm((s) => ({ ...s, mode: e.target.value as any }))} disabled={!!editingVisitId}>
                      <option value="WALK_IN">현장 접수</option>
                      <option value="RESERVATION">예약내원 접수</option>
                    </select>
                  </label>
                  <label>
                    <span>성별</span>
                    <select value={visitForm.gender} onChange={(e) => setVisitForm((s) => ({ ...s, gender: e.target.value as "M" | "F" }))}>
                      <option value="M">남(M)</option><option value="F">여(F)</option>
                    </select>
                  </label>
                  <div className="info-pill">
                    {/* [FIXED P4] 상태는 시스템 자동 부여 — 수동 선택 금지 */}
                    {/* 등록: 항상 WAITING / 수정: 현재 상태 표시만 (변경 불가) */}
                    <span>상태</span>
                    <strong>{editingVisitId ? (STATUS_LABEL[visitForm.status] ?? visitForm.status) : "대기 (자동)"}</strong>
                    <small>{editingVisitId ? "시스템 자동 관리" : "접수 시 자동 대기 설정"}</small>
                  </div>
                </div>

                {visitForm.mode === "RESERVATION" && !editingVisitId && (
                  <div className="form-grid">
                    <label>
                      <span>예약 선택</span>
                      <select value={visitForm.reservationId ?? ""} onChange={(e) => {
                        const reservationId = Number(e.target.value);
                        const r = state.reservations.find((it: any) => it.id === reservationId);
                        const p = r ? patientsById[r.patientId] : undefined;
                        setVisitForm((s) => ({
                          ...s,
                          reservationId,
                          patientName: p?.name ?? s.patientName,
                          gender: p?.gender ?? s.gender,
                          rrnFront: p?.rrnFront ?? s.rrnFront,
                          rrnBack: p?.rrnBack ?? s.rrnBack,
                          phone: p?.phone ?? s.phone,
                        }));
                      }}>
                        <option value="">예약 선택</option>
                        {activeReservations.map((r: any) => {
                          const p = patientsById[r.patientId];
                          return <option key={r.id} value={r.id}>{r.id} / {p?.name ?? r.contactName} / {formatDateTime(r.reservedAt)}</option>;
                        })}
                      </select>
                    </label>
                  </div>
                )}

                <div className="form-grid tri">
                  <label><span>환자명</span><input value={visitForm.patientName} onChange={(e) => setVisitForm((s) => ({ ...s, patientName: e.target.value }))} /></label>
                  <label><span>전화번호</span><div className="phone-split"><input value="010" readOnly /><input ref={visitPhoneMidRef} inputMode="numeric" maxLength={4} value={visitPhoneParts.mid} onChange={(e) => { const v = e.target.value; setVisitForm((s) => ({ ...s, phone: joinPhone(v, splitPhone(s.phone).last) })); if (digitsOnly(v).length >= 4) visitPhoneLastRef.current?.focus(); }} placeholder="1234" /><input ref={visitPhoneLastRef} inputMode="numeric" maxLength={4} value={visitPhoneParts.last} onChange={(e) => setVisitForm((s) => ({ ...s, phone: joinPhone(splitPhone(s.phone).mid, e.target.value) }))} placeholder="5678" /></div></label>
                  <div className="button-cell"><div className="info-panel"><strong>접수번호</strong> {editingVisitId ? editingVisitId : "자동생성"}</div></div>
                </div>
                <div className="form-grid tri">
                  <label><span>주민번호 앞자리</span><input value={visitForm.rrnFront} onChange={(e) => setVisitForm((s) => ({ ...s, rrnFront: e.target.value.replace(/\D/g, "").slice(0,6) }))} placeholder="YYMMDD" /></label>
                  <label><span>주민번호 뒷자리</span><input type={rrnVisible ? "text" : "password"} inputMode="numeric" value={visitForm.rrnBack} onChange={(e) => setVisitForm((s) => ({ ...s, rrnBack: e.target.value.replace(/\D/g, "").slice(0,7) }))} maxLength={7} /></label>
                  <div className="button-cell"><button type="button" onClick={() => setRrnVisible((v) => !v)}>{rrnVisible ? "마스킹" : "전체보기"}</button></div>
                </div>

                <div className="button-row">
                  <button type="button" className="primary-btn" onClick={handleVisitSave} disabled={!capacity.canRegister && !editingVisitId}>{editingVisitId ? "접수 수정" : "접수 등록"}</button>
                  {editingVisitId && <button type="button" onClick={resetVisitForm}>등록 모드</button>}
                </div>
              </GlassCard>

              <GlassCard
                title="접수 목록"
                subtitle="목록에서는 이름/주민번호 마스킹 표시 · 전체보기 없음"
                className="nested-card"
                right={
                  <button type="button" onClick={() => visitsQuery.refetch()} disabled={visitsQuery.isFetching}>
                    {visitsQuery.isFetching ? "조회 중..." : "접수 조회"}
                  </button>
                }
              >
                <div className="table-wrap">
                  <table className="ui-table">
                    <thead><tr><th>접수번호</th><th>환자명</th><th>성별</th><th>주민번호</th><th>상태</th><th>등록시각</th><th>접수유형</th><th>관리</th></tr></thead>
                    <tbody>
                      {receptionRows.map((visit: any) => {
                        const p = patientsById[visit.patientId];
                        const displayName = isServerData
                          ? maskName(visit.patientName ?? '-')
                          : (p ? maskName(p.name) : '-');
                        const displayGender = isServerData
                          ? (visit.gender === 'M' ? '남(M)' : visit.gender === 'F' ? '여(F)' : '-')
                          : (p ? (p.gender === 'M' ? '남(M)' : '여(F)') : '-');
                        const displayRrn = isServerData
                          ? (visit.rrnMasked ?? '******-*******')
                          : (p ? formatRrnMasked(p.rrnFront, p.rrnBack) : '-');
                        if (!isServerData && !p) return null;
                        return (
                          <tr key={visit.id}>
                            <td>{visit.id}</td>
                            <td>{displayName}</td>
                            <td>{displayGender}</td>
                            <td>{displayRrn}</td>
                            <td>{STATUS_LABEL[visit.status] ?? visit.status}</td>
                            <td>{formatDateTime(visit.registeredAt)}</td>
                            <td>{visit.visitType === "RESERVATION" ? "예약내원" : "현장"}</td>
                            <td><div className="inline-btns">
                              {/* WAITING 상태만 수정/삭제 허용 — 서버 cancel()이 WAITING만 허용 (409 원천 차단) */}
                              {visit.status === "WAITING" && (
                                <button type="button" onClick={() => selectVisitForEdit(visit)}>수정</button>
                              )}{visit.status === "WAITING" && <button type="button" onClick={async () => {
                              try {
                                if (serverWriteEnabled) {
                                  try {
                                    await cancelVisitServer({ session: state.session ?? undefined, visitId: visit.id, reason: "UI 삭제" });
                                  } catch (serverErr: any) {
                                    const msg = String(serverErr?.message ?? "");
                                    // [FIXED] 409 = 이미 서버에서 CANCELED 상태
                                    // → 서버 취소는 이미 됐으므로 로컬 삭제 + 목록 갱신은 진행
                                    if (!msg.includes("409") && !msg.toLowerCase().includes("conflict")) {
                                      showToast(`접수 서버 취소 실패: ${msg}`);
                                      return;
                                    }
                                  }
                                  // 성공 또는 409(이미 취소) 모두 → 로컬 삭제 + 목록 즉시 갱신
                                  removeVisitEntry(visit.id);
                                  await visitsQuery.refetch();
                                  showToast("삭제 완료 (서버 반영)");
                                } else {
                                  showToast(removeVisitEntry(visit.id).message);
                                }
                              } catch (e: any) {
                                showToast(`삭제 실패: ${e?.message || e}`);
                              }
                            }}>삭제</button>}</div></td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </GlassCard>
            </div>
          )}

          {tab === "EMERGENCY" && (
            <div className="split-grid emergency-grid">
              <GlassCard title="응급 환자 수" subtitle="응급 병상 수동 조정 (운영자 직접 설정)" className="nested-card">
                <div className="counter-panel">
                  <div className="counter-big">{state.emergencyCount}명</div>
                  <input type="range" min={0} max={10} value={emergencyValue} onChange={(e) => setEmergencyValue(Number(e.target.value))} />
                  <div className="counter-actions">
                    <button type="button" onClick={() => setEmergencyValue((v) => Math.max(0, v - 1))}>-1</button>
                    <button type="button" onClick={() => setEmergencyValue((v) => Math.min(10, v + 1))}>+1</button>
                    <button type="button" className="primary-btn" onClick={async () => {
                      const result = setEmergencyCount(emergencyValue);
                      showToast(result.message);
                      if (state.session?.authSource === "server") {
                        try {
                          await updateEmergencyCount(emergencyValue);
                        } catch (e) {
                          showToast("서버 저장 실패 (로컬만 적용됨)");
                        }
                      }
                    }}>적용</button>
                  </div>
                </div>
              </GlassCard>
              <GlassCard title="운영 상태" className="nested-card">
                <div className={`capacity-indicator ${capacity.level.toLowerCase()}`}><div className="capacity-indicator__ring" /><div><strong>{capacity.current} / 30명</strong><p>대기+진료중 {capacity.waitingAndInTreatment} · 예약 {capacity.reservation} · 응급 {capacity.emergency}</p></div></div>
              </GlassCard>
            </div>
          )}

          {toast && <div className="toast-mini">{toast}</div>}
        </GlassCard>
      </div>
    </RoleGate>
  );
}
