"use client";

// [MODIFIED] 날짜 기본값 → 오늘 KST 기준
function todayKST(): Date {
  return new Date(Date.now() + 9 * 60 * 60 * 1000);
}
function todayStr(): string {
  return todayKST().toISOString().slice(0, 10);
}
function todayPlusDays(n: number): string {
  return new Date(Date.now() + 9 * 60 * 60 * 1000 + n * 86400000).toISOString().slice(0, 10);
}
function todayNextHour(): string {
  const d = todayKST();
  d.setUTCMinutes(0, 0, 0);
  d.setUTCHours(d.getUTCHours() + 1);
  return d.toISOString().slice(0, 16);
}

import { useEffect, useMemo, useRef, useState } from "react";
import { GlassCard } from "@/shared/components/GlassCard";
import { RoleGate } from "@/shared/components/RoleGate";
import { StatusBadge } from "@/shared/components/StatusBadge";
import { useHospital } from "@/shared/store/HospitalStore";
import { formatDateTime } from "@/shared/lib/date";
import { formatRrnMasked, maskName, maskPhone } from "@/shared/lib/masking";
import { STATUS_LABEL } from "@/shared/config/constants";
import type { VisitStatus } from "@/shared/types/domain";
import { cancelVisitServer, checkInReservationServer, createReservationServer, createVisitServer, updateVisitServer, upsertPatientForReception } from "@/shared/services/receptionMutationApi";

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

  const [serverWriteEnabled, setServerWriteEnabled] = useState(false);

  const [tab, setTab] = useState<ReceptionTab>("RESERVATION");
  const [toast, setToast] = useState("");

  const [reservationForm, setReservationForm] = useState({ name: "", phone: "", reservedAt: todayNextHour() });
  const [editingReservationId, setEditingReservationId] = useState<number | null>(null);

  const activeReservations = useMemo(
    () => state.reservations.filter((r: any) => r.status === "RESERVED").sort((a: any, b: any) => a.reservedAt.localeCompare(b.reservedAt)),
    [state.reservations]
  );

  const receptionRows = useMemo(() => state.visits.slice().sort((a: any, b: any) => b.id - a.id), [state.visits]);

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
    setReservationForm({ name: "", phone: "", reservedAt: todayNextHour() });
  };

  const resetVisitForm = () => {
    setEditingVisitId(null);
    setRrnVisible(false);
    setVisitForm({ mode: "WALK_IN", patientName: "", gender: "M", rrnFront: "", rrnBack: "", phone: "", status: "WAITING" });
  };

  const handleReservationSave = async () => {
    const iso = new Date(reservationForm.reservedAt).toISOString();
    try {
      if (serverWriteEnabled && !editingReservationId) {
        const tempPatientId = Number(`${Date.now()}`.slice(-10));
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

  const handleRegisterReservationVisit = async (reservationId: number) => {
    const r = state.reservations.find((it: any) => it.id === reservationId);
    if (!r) return;
    const p = patientsById[r.patientId];
    if (!p) return;
    try {
      if (serverWriteEnabled) {
        await checkInReservationServer({ session: state.session ?? undefined, reservationId });
      }
      const result = registerVisitEntry({
        mode: "RESERVATION",
        reservationId,
        patientName: p.name,
        gender: p.gender,
        rrnFront: p.rrnFront,
        rrnBack: p.rrnBack,
        phone: p.phone,
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
          const tempPatientId = Number(`${Date.now()}`.slice(-10));
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
    if (!p) return;
    setEditingVisitId(visit.id);
    setRrnVisible(false);
    setVisitForm({
      mode: visit.visitType,
      reservationId: visit.sourceReservationId,
      patientName: p.name,
      gender: p.gender,
      rrnFront: p.rrnFront,
      rrnBack: p.rrnBack,
      phone: p.phone,
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
                  <label><span>예약시간대</span><input type="datetime-local" value={reservationForm.reservedAt} onChange={(e) => setReservationForm((s) => ({ ...s, reservedAt: e.target.value }))} /></label>
                </div>
                <div className="button-row">
                  <button type="button" className="primary-btn" onClick={handleReservationSave} disabled={!capacity.canRegister && !editingReservationId}>{editingReservationId ? "예약 수정" : "예약 등록"}</button>
                  {editingReservationId && <button type="button" onClick={resetReservationForm}>신규 모드</button>}
                </div>
              </GlassCard>

              <GlassCard title="예약 현황" subtitle="이름/전화번호 마스킹 · 수정/예약내원접수" className="nested-card">
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
                  <label>
                    <span>상태 {editingVisitId ? "(수정 가능)" : "(등록 시 대기 고정)"}</span>
                    <select value={visitForm.status} onChange={(e) => setVisitForm((s) => ({ ...s, status: e.target.value as VisitStatus }))} disabled={!editingVisitId}>
                      <option value="WAITING">대기</option><option value="IN_TREATMENT">진료중</option><option value="COMPLETED">완료</option>
                    </select>
                  </label>
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

              <GlassCard title="접수 목록" subtitle="목록에서는 이름/주민번호 마스킹 표시 · 전체보기 없음" className="nested-card">
                <div className="table-wrap">
                  <table className="ui-table">
                    <thead><tr><th>접수번호</th><th>환자명</th><th>성별</th><th>주민번호</th><th>상태</th><th>등록시각</th><th>접수유형</th><th>관리</th></tr></thead>
                    <tbody>
                      {receptionRows.map((visit: any) => {
                        const p = patientsById[visit.patientId];
                        if (!p) return null;
                        return (
                          <tr key={visit.id}>
                            <td>{visit.id}</td>
                            <td>{maskName(p.name)}</td>
                            <td>{p.gender === "M" ? "남(M)" : "여(F)"}</td>
                            <td>{formatRrnMasked(p.rrnFront, p.rrnBack)}</td>
                            <td>{STATUS_LABEL[visit.status]}</td>
                            <td>{formatDateTime(visit.registeredAt)}</td>
                            <td>{visit.visitType === "RESERVATION" ? "예약내원" : "현장"}</td>
                            <td><div className="inline-btns"><button type="button" onClick={() => selectVisitForEdit(visit)}>수정</button><button type="button" onClick={async () => { try { if (serverWriteEnabled) await cancelVisitServer({ session: state.session ?? undefined, visitId: visit.id, reason: "UI 삭제" }); showToast(removeVisitEntry(visit.id).message + (serverWriteEnabled ? " (서버 취소 포함)" : "")); } catch (e: any) { showToast(`접수 서버 취소 실패: ${e?.message || e}`); } }}>삭제</button></div></td>
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
              <GlassCard title="응급 환자 수" subtitle="0~10 범위" className="nested-card">
                <div className="counter-panel">
                  <div className="counter-big">{state.emergencyCount}명</div>
                  <input type="range" min={0} max={10} value={emergencyValue} onChange={(e) => setEmergencyValue(Number(e.target.value))} />
                  <div className="counter-actions">
                    <button type="button" onClick={() => setEmergencyValue((v) => Math.max(0, v - 1))}>-1</button>
                    <button type="button" onClick={() => setEmergencyValue((v) => Math.min(10, v + 1))}>+1</button>
                    <button type="button" className="primary-btn" onClick={() => showToast(setEmergencyCount(emergencyValue).message)}>적용</button>
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
