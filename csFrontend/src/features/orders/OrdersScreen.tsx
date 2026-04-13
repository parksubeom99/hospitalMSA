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

import { useEffect, useMemo, useState } from "react";
import { GlassCard } from "@/shared/components/GlassCard";
import { RoleGate } from "@/shared/components/RoleGate";
import { computeAdmission, MEDICATION_CATALOG, useHospital } from "@/shared/store/HospitalStore";
import type { FinalOrderInjectionItem, FinalOrderMedicationItem } from "@/shared/types/domain";
import { calcNights, periodLabel } from "@/shared/lib/date";
import { formatCurrency } from "@/shared/lib/format";
import { getFinalOrdersByVisitServer, saveAndFinalizeFinalOrdersServer } from "@/shared/services/clinicalApi";

type FinalOrderType = "MED" | "SURGERY" | "ADMISSION" | "INJECTION" | "NONE";

const INJECTION_CATALOG: FinalOrderInjectionItem[] = [
  { injectionCode: "INJ001", injectionName: "생리식염주사" },
  { injectionCode: "INJ002", injectionName: "진통주사" },
  { injectionCode: "INJ003", injectionName: "소염주사" },
  { injectionCode: "INJ004", injectionName: "비타민주사" },
];

export function OrdersScreen() {
  const { state, patientsById, saveFinalOrder } = useHospital();

  // [FIX] SOAP 작성 + 검사오더 신청 완료한 환자만 최종오더 화면에 표시
  // 조건: soaps에 해당 visitId 키가 있고(SOAP 작성됨),
  //       examOrders에 해당 visitId가 있고 항목이 1개 이상(검사오더 완료)
  const activeVisits = useMemo(
    () => state.visits
      .filter(v => {
        if (v.status === "COMPLETED") return false;
        const hasSoap = v.id in state.soaps;
        const hasExamOrder = Array.isArray(state.examOrders[v.id]) && state.examOrders[v.id].length > 0;
        return hasSoap && hasExamOrder;
      })
      .sort((a, b) => b.id - a.id),
    [state.visits, state.soaps, state.examOrders]
  );
  const [visitId, setVisitId] = useState<number>(activeVisits[0]?.id ?? 0);
  const [types, setTypes] = useState<FinalOrderType[]>([]);
  const [medRows, setMedRows] = useState<FinalOrderMedicationItem[]>([]);
  const [injectionRows, setInjectionRows] = useState<FinalOrderInjectionItem[]>([]);
  const [surgeryType, setSurgeryType] = useState<"INTERNAL" | "EXTERNAL">("INTERNAL");
  const [roomNo, setRoomNo] = useState<number>(1);
  const [wardNo, setWardNo] = useState<number>(1);
  const [admitDate, setAdmitDate] = useState<string>(todayStr());
  const [dischargeDate, setDischargeDate] = useState<string>(todayPlusDays(3));
  const [message, setMessage] = useState("");
  const [serverWriteEnabled, setServerWriteEnabled] = useState(false);
  const [serverSyncEnabled, setServerSyncEnabled] = useState(false);
  const [syncLoading, setSyncLoading] = useState(false);
  const [serverFinalSummary, setServerFinalSummary] = useState<string>("미동기화");

  useEffect(() => {
    const fo = state.finalOrders[visitId];
    if (!fo) return;
    setTypes([...fo.types] as FinalOrderType[]);
    setMedRows(fo.medications ?? []);
    setInjectionRows(fo.injections ?? []);
    setSurgeryType(fo.surgery?.surgeryType ?? "INTERNAL");
    setRoomNo(fo.surgery?.roomNo ?? 1);
    setWardNo(fo.admission?.wardNo ?? 1);
    setAdmitDate(fo.admission?.admitDate ?? todayStr());
    setDischargeDate(fo.admission?.dischargeDate ?? todayPlusDays(3));
  }, [visitId, state.finalOrders]);

  const visit = activeVisits.find(v => v.id === visitId);
  const patient = visit ? patientsById[visit.patientId] : undefined;
  const nights = calcNights(admitDate, dischargeDate);

  const toggleType = (type: FinalOrderType) => {
    setTypes((prev) => {
      const has = prev.includes(type);
      if (type === "NONE") return has ? prev.filter(t => t !== "NONE") : ["NONE"];
      const cleaned = prev.filter(t => t !== "NONE");
      return has ? cleaned.filter(t => t !== type) : [...cleaned, type];
    });
  };

  const addMedication = () => {
    const item = MEDICATION_CATALOG[0];
    setMedRows((prev) => [...prev, { drugCode: item.drugCode, drugName: item.drugName, drugGroup: item.drugGroup, qty: 1 }]);
  };

  const updateMed = (idx: number, patch: Partial<FinalOrderMedicationItem>) => {
    setMedRows((prev) => prev.map((m, i) => (i === idx ? { ...m, ...patch } : m)));
  };

  const removeMed = (idx: number) => setMedRows((prev) => prev.filter((_, i) => i !== idx));

  const addInjection = () => {
    const item = INJECTION_CATALOG[0];
    setInjectionRows((prev) => [...prev, { ...item }]);
  };
  const updateInjection = (idx: number, code: string) => {
    const item = INJECTION_CATALOG.find((it) => it.injectionCode === code);
    if (!item) return;
    setInjectionRows((prev) => prev.map((r, i) => (i === idx ? { ...item } : r)));
  };
  const removeInjection = (idx: number) => setInjectionRows((prev) => prev.filter((_, i) => i !== idx));

  const preview = useMemo(() => {
    let amount = 0;
    const rows: Array<{ label: string; amount: number }> = [];
    if (types.includes("MED")) {
      const qty = medRows.reduce((s, r) => s + (Number(r.qty) || 0), 0);
      const medAmt = qty * 1000;
      amount += medAmt;
      rows.push({ label: `약제비 (${qty}개 × 1,000원)`, amount: medAmt });
    }
    if (types.includes("INJECTION")) {
      const injAmt = injectionRows.length * 5000;
      amount += injAmt;
      rows.push({ label: `주사비 (${injectionRows.length}건 × 5,000원)`, amount: injAmt });
    }
    if (types.includes("SURGERY")) {
      const surgeryAmt = surgeryType === "INTERNAL" ? 50000 : 100000;
      amount += surgeryAmt;
      rows.push({ label: `${surgeryType === "INTERNAL" ? "내과수술" : "외과수술"} (수술실 ${roomNo}번)`, amount: surgeryAmt });
    }
    if (types.includes("ADMISSION")) {
      const admitAmt = Math.max(0, nights) * 10000;
      amount += admitAmt;
      rows.push({ label: `입원비 (${Math.max(0, nights)}박 × 10,000원, 병동 ${wardNo}번)`, amount: admitAmt });
    }
    if (types.includes("NONE")) rows.push({ label: "이상소견없음 (즉시 완료)", amount: 0 });
    return { rows, amount };
  }, [types, medRows, injectionRows, surgeryType, roomNo, wardNo, nights]);

  const emit = (msg: string) => {
    setMessage(msg);
    window.setTimeout(() => setMessage(""), 2200);
  };


  const syncFinalOrdersFromServer = async () => {
    if (!state.session?.accessToken) return emit("실서버 IAM 로그인 후 동기화 가능합니다.");
    if (!visitId) return emit("접수를 먼저 선택해주세요.");
    try {
      setSyncLoading(true);
      const rows = await getFinalOrdersByVisitServer({ session: state.session ?? undefined, visitId });
      if (!rows.length) {
        setServerFinalSummary('실서버 최종오더 없음');
      } else {
        setServerFinalSummary(rows.map((r: any) => `${r.type}:${r.status}`).join(', '));
      }
      emit(`실서버 최종오더 동기화 완료 (${rows.length}건)`);
    } catch (e: any) {
      emit(`실서버 동기화 실패: ${e?.message || e}`);
    } finally {
      setSyncLoading(false);
    }
  };

  useEffect(() => {
    if (!serverSyncEnabled || !visitId) return;
    void syncFinalOrdersFromServer();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [serverSyncEnabled, visitId]);

  return (
    <RoleGate allowed={["DOC", "SYS"]}>
      <div className="page-grid page-grid--readable">
        <GlassCard title="오더 (최종처방)" subtitle="약 / 주사 / 수술 / 입원 / 이상소견없음(NONE)">
          <div className="form-grid tri">
            <div className="inline-check-group" style={{ gridColumn: "1 / -1" }}>
              <button type="button" onClick={() => { setServerWriteEnabled(true); setServerSyncEnabled(true); void syncFinalOrdersFromServer(); }} disabled={syncLoading}>{syncLoading ? "동기화 중..." : "서버 동기화"}</button>
              <small className="muted">서버요약: {serverFinalSummary}</small>
            </div>
            <label>
              <span>접수 선택</span>
              <select value={visitId} onChange={(e) => setVisitId(Number(e.target.value))}>
                {activeVisits.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.id} / {patientsById[v.patientId]?.name ?? "-"}
                  </option>
                ))}
              </select>
            </label>
            <div className="info-pill">
              <span>환자</span>
              <strong>{patient ? patient.name : "선택 없음"}</strong>
              <small>복수 선택 가능, 단 NONE은 단독</small>
            </div>
            <div className="inline-check-group">
              {(["MED", "SURGERY", "ADMISSION", "INJECTION", "NONE"] as FinalOrderType[]).map((t) => (
                <label key={t} className={`pill-check ${types.includes(t) ? "is-on" : ""}`}>
                  <input type="checkbox" checked={types.includes(t)} onChange={() => toggleType(t)} />
                  <span>{t === "MED" ? "약제" : t === "SURGERY" ? "수술" : t === "ADMISSION" ? "입원" : t === "INJECTION" ? "주사" : "이상소견없음"}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="split-grid">
            <div className="stack-col">
              <GlassCard title="약제" subtitle="대표 약품 10종 / 종류 및 수량 선택" className="nested-card">
                <div className="button-row">
                  <button type="button" onClick={addMedication} disabled={!types.includes("MED") || types.includes("NONE")}>약제 추가</button>
                </div>
                <div className="med-list">
                  {medRows.map((row, idx) => (
                    <div key={`${row.drugCode}-${idx}`} className="med-row">
                      <select
                        value={row.drugCode}
                        onChange={(e) => {
                          const c = MEDICATION_CATALOG.find((m) => m.drugCode === e.target.value);
                          if (!c) return;
                          updateMed(idx, { drugCode: c.drugCode, drugName: c.drugName, drugGroup: c.drugGroup });
                        }}
                        disabled={!types.includes("MED") || types.includes("NONE")}
                      >
                        {MEDICATION_CATALOG.map((m) => (
                          <option key={m.drugCode} value={m.drugCode}>{m.drugName} ({m.drugGroup})</option>
                        ))}
                      </select>
                      <input
                        type="number"
                        min={1}
                        value={row.qty}
                        disabled={!types.includes("MED") || types.includes("NONE")}
                        onChange={(e) => updateMed(idx, { qty: Math.max(1, Number(e.target.value)) })}
                      />
                      <button type="button" onClick={() => removeMed(idx)}>삭제</button>
                    </div>
                  ))}
                  {medRows.length === 0 && <div className="muted">추가된 약제가 없습니다.</div>}
                </div>
              </GlassCard>

              <GlassCard title="주사" subtitle="주사명 선택 / 수량 1개 고정 / 단가 5,000원" className="nested-card">
                <div className="button-row">
                  <button type="button" onClick={addInjection} disabled={!types.includes("INJECTION") || types.includes("NONE")}>주사 추가</button>
                </div>
                <div className="med-list">
                  {injectionRows.map((row, idx) => (
                    <div key={`${row.injectionCode}-${idx}`} className="med-row">
                      <select value={row.injectionCode} onChange={(e) => updateInjection(idx, e.target.value)} disabled={!types.includes("INJECTION") || types.includes("NONE")}>
                        {INJECTION_CATALOG.map((m) => <option key={m.injectionCode} value={m.injectionCode}>{m.injectionName}</option>)}
                      </select>
                      <input value={1} readOnly disabled />
                      <button type="button" onClick={() => removeInjection(idx)}>삭제</button>
                    </div>
                  ))}
                  {injectionRows.length === 0 && <div className="muted">추가된 주사가 없습니다.</div>}
                </div>
              </GlassCard>

              <GlassCard title="수술" subtitle="수술실 1~5 / 내과수술·외과수술" className="nested-card">
                <div className="form-grid tri">
                  <label>
                    <span>수술 종류</span>
                    <select value={surgeryType} onChange={(e) => setSurgeryType(e.target.value as "INTERNAL" | "EXTERNAL")} disabled={!types.includes("SURGERY") || types.includes("NONE")}>
                      <option value="INTERNAL">내과수술</option>
                      <option value="EXTERNAL">외과수술</option>
                    </select>
                  </label>
                  <label>
                    <span>수술실</span>
                    <select value={roomNo} onChange={(e) => setRoomNo(Number(e.target.value))} disabled={!types.includes("SURGERY") || types.includes("NONE")}>
                      {Array.from({ length: 5 }, (_, i) => i + 1).map((n) => <option key={n} value={n}>{n}번 수술실</option>)}
                    </select>
                    </label>
                </div>
              </GlassCard>

              <GlassCard title="입원" subtitle="병동 1~10 / 기간 선택 / n박 계산" className="nested-card">
                <div className="form-grid tri">
                  <label>
                    <span>병동</span>
                    <select value={wardNo} onChange={(e) => setWardNo(Number(e.target.value))} disabled={!types.includes("ADMISSION") || types.includes("NONE")}>
                      {Array.from({ length: 10 }, (_, i) => i + 1).map((n) => <option key={n} value={n}>{n}번 병동</option>)}
                    </select>
                  </label>
                  <label>
                    <span>입원 시작일</span>
                    <input type="date" value={admitDate} onChange={(e) => setAdmitDate(e.target.value)} disabled={!types.includes("ADMISSION") || types.includes("NONE")} />
                  </label>
                  <label>
                    <span>퇴원일</span>
                    <input type="date" value={dischargeDate} onChange={(e) => setDischargeDate(e.target.value)} disabled={!types.includes("ADMISSION") || types.includes("NONE")} />
                  </label>
                </div>
                <div className="info-panel">
                  <div><strong>기간</strong> {periodLabel(admitDate, dischargeDate)} ({Math.max(0, nights)}박)</div>
                </div>
              </GlassCard>
            </div>

            <GlassCard title="최종오더 미리보기" subtitle="수납 화면 자동 산출 기준 확인" className="nested-card">
              <div className="summary-box">
                {preview.rows.map((r, i) => (
                  <div key={i} className="summary-row">
                    <span>{r.label}</span>
                    <strong>{formatCurrency(r.amount)}</strong>
                  </div>
                ))}
                {preview.rows.length === 0 && <div className="muted">선택된 항목이 없습니다.</div>}
                <hr />
                <div className="summary-row total">
                  <span>합계</span>
                  <strong>{formatCurrency(preview.amount)}</strong>
                </div>
              </div>

              <div className="button-row">
                <button
                  className="primary-btn"
                  type="button"
                  onClick={async () => {
                    const admission = types.includes("ADMISSION")
                      ? computeAdmission({ wardNo, admitDate, dischargeDate })
                      : undefined;
                    try {
                     const session = state.session;

 if (serverWriteEnabled) {
  if (!session) {
    emit("실서버 저장/확정 모드는 IAM 로그인 후 사용 가능합니다.");
    return;
  }

  const noteParts = [
    types.includes("MED") ? `MED:${medRows.length}` : null,
    types.includes("INJECTION") ? `INJECTION:${injectionRows.length}` : null,
    types.includes("SURGERY") ? `SURGERY:${surgeryType}-R${roomNo}` : null,
    types.includes("ADMISSION") && admission ? `ADMISSION:W${wardNo},${admission.nights}N` : null,
    types.includes("NONE") ? "NONE" : null,
  ]
    .filter(Boolean)
    .join(" | ");

  await saveAndFinalizeFinalOrdersServer({
    session,
    visitId,
    types,
    note: noteParts,
  });
}
                      const result = saveFinalOrder({
                        visitId,
                        types,
                        medications: types.includes("MED") ? medRows.filter((m) => m.qty > 0) : [],
                        injections: types.includes("INJECTION") ? injectionRows : [],
                        surgery: types.includes("SURGERY") ? { surgeryType, roomNo } : undefined,
                        admission,
                      });
                      emit(result.message + (serverWriteEnabled ? " (서버 저장/확정 포함)" : ""));
                    } catch (e: any) {
                      emit(`최종오더 서버 저장 실패: ${e?.message || e}`);
                    }
                  }}
                >
                  최종오더 저장
                </button>
              </div>

              <GlassCard title="입원목록 조회(미리보기)" className="nested-card">
                <div className="table-wrap">
                  <table className="ui-table compact">
                    <thead>
                      <tr>
                        <th>환자명</th>
                        <th>성별</th>
                        <th>주민번호</th>
                        <th>입원기간</th>
                        <th>박수</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.values(state.finalOrders)
                        .filter((fo) => fo.types.includes("ADMISSION") && fo.admission)
                        .map((fo) => {
                          const v = state.visits.find((it) => it.id === fo.visitId);
                          const p = v ? patientsById[v.patientId] : undefined;
                          if (!p || !fo.admission) return null;
                          return (
                            <tr key={fo.visitId}>
                              <td>{p.name[0]}*{p.name[p.name.length - 1]}</td>
                              <td>{p.gender === "M" ? "남(M)" : "여(F)"}</td>
                              <td>{p.rrnFront}-{p.rrnBack[0]}******</td>
                              <td>{periodLabel(fo.admission.admitDate, fo.admission.dischargeDate)}</td>
                              <td>{fo.admission.nights}박</td>
                            </tr>
                          );
                        })}
                    </tbody>
                  </table>
                </div>
              </GlassCard>
            </GlassCard>
          </div>

          {message && <div className="toast-mini">{message}</div>}
        </GlassCard>
      </div>
    </RoleGate>
  );
}
