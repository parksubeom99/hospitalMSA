"use client";

import { useEffect, useMemo, useState } from "react";
import { GlassCard } from "@/shared/components/GlassCard";
import { RoleGate } from "@/shared/components/RoleGate";
import { normalizeExamSelection, useHospital, EXAM_OPTIONS } from "@/shared/store/HospitalStore";
import type { ExamCategory, ExamOrderItem } from "@/shared/types/domain";
import { formatRrnMasked } from "@/shared/lib/masking";
import { STATUS_LABEL } from "@/shared/config/constants";
import { saveExamOrdersByVisitServer, saveSoapServer } from "@/shared/services/clinicalApi";
import { useExamOrdersQuery, useSoapQuery, useVisitClinicalStatusListQuery } from "@/shared/services/clinical/clinicalQueries"; // [MODIFIED]

export function ClinicalScreen() {
  const { state, patientsById, saveSoap, saveExamOrders } = useHospital();
  // [MODIFIED] 서버 visit_clinical_status 우선, 로컬 fallback
  const isServerSession = state.session?.authSource === "server";
  const visitClinicalListQuery = useVisitClinicalStatusListQuery({ enabled: isServerSession });
  const isServerVisitData = isServerSession
    && Array.isArray(visitClinicalListQuery.data)
    && visitClinicalListQuery.data.length > 0;

  // 서버 데이터: visit_clinical_status 목록 (BILLED/BILLING_FAILED 제외됨)
  // 로컬 fallback: HospitalStore.visits (COMPLETED 제외)
  const activeVisits = useMemo(() => {
    if (isServerVisitData) {
      return visitClinicalListQuery.data!.map(s => ({
        id: s.visitId,
        status: s.clinicalStatus,
        patientId: state.visits.find(v => v.id === s.visitId)?.patientId ?? 0,
        patientName: s.patientName, // [FIXED B3] 서버 이름 보존
      }));
    }
    return state.visits
      .filter(v => v.status !== "COMPLETED")
      .sort((a, b) => b.id - a.id)
      .map(v => ({ id: v.id, status: v.status, patientId: v.patientId, patientName: undefined }));
  }, [isServerVisitData, visitClinicalListQuery.data, state.visits]);

  const [visitId, setVisitId] = useState<number>(0);

  // [ADDED] 서버 데이터 로드 완료 시 첫 번째 항목 자동 선택
  useEffect(() => {
    if (activeVisits.length > 0 && visitId === 0) {
      setVisitId(activeVisits[0].id);
    }
  }, [activeVisits]);
  const currentSoap = state.soaps[visitId];
  const [soap, setSoap] = useState({
    subjective: "",
    objective: "",
    assessment: "",
    plan: "",
  });
  const [selectedItems, setSelectedItems] = useState<ExamOrderItem[]>([]);
  const [message, setMessage] = useState("");
  // [MODIFIED] 체크박스 2개(실서버 저장/동기화 모드) → 동기화 버튼 1개로 단순화
  // 실서버 저장: 세션이 실서버 로그인(accessToken 존재)이면 자동으로 서버 저장 시도
  // 실서버 동기화: "동기화 실행" 버튼 클릭 시에만 수행
  const serverWriteEnabled = isServerSession; // [MODIFIED] isServerSession으로 통합
  // [MODIFIED] useState(syncLoading/serverSyncedAt) → React Query로 대체
  const soapQuery = useSoapQuery({ visitId }); // [ADDED]
  const examOrdersQuery = useExamOrdersQuery({ visitId }); // [ADDED]
  const syncLoading = soapQuery.isFetching || examOrdersQuery.isFetching; // [MODIFIED]
  const [serverSyncedAt, setServerSyncedAt] = useState<string | null>(null);

  useEffect(() => {
    setSoap({
      subjective: currentSoap?.subjective ?? "",
      objective: currentSoap?.objective ?? "",
      assessment: currentSoap?.assessment ?? "",
      plan: currentSoap?.plan ?? "",
    });
    setSelectedItems(state.examOrders[visitId] ?? []);
  }, [visitId, currentSoap, state.examOrders]);

  const visit = activeVisits.find((v) => v.id === visitId);
  const patient = visit ? patientsById[visit.patientId] : undefined;

  const toggleItem = (category: ExamCategory, item: ExamOrderItem) => {
    setSelectedItems((prev) => {
      const exists = prev.some((p) => p.code === item.code);
      let next = exists ? prev.filter((p) => p.code !== item.code) : [...prev, item];
      const noneCode = `${category}_NONE`;
      const isNone = item.code === noneCode;
      if (isNone && !exists) {
        next = next.filter((p) => p.category !== category || p.code === noneCode);
      } else if (!isNone) {
        next = next.filter((p) => !(p.category === category && p.code === noneCode));
      }
      return normalizeExamSelection(next);
    });
  };

  const emit = (msg: string) => {
    setMessage(msg);
    window.setTimeout(() => setMessage(""), 1800);
  };


  // [MODIFIED] 직접 fetch → React Query refetch() 위임
  const syncClinicalFromServer = async () => {
    if (!isServerSession) return emit("실서버 IAM 로그인 후 동기화 가능합니다.");
    if (!visitId) return emit("접수를 먼저 선택해주세요.");
    try {
      const [soapResult, examResult] = await Promise.all([ // [MODIFIED]
        soapQuery.refetch(),
        examOrdersQuery.refetch(),
      ]);
      const soapRes = soapResult.data;
      const examRes = examResult.data ?? [];
      if (soapRes) {
        setSoap({
          subjective: soapRes.subjective ?? "", objective: soapRes.objective ?? "",
          assessment: soapRes.assessment ?? "", plan: soapRes.plan ?? "",
        });
      }
      setSelectedItems(examRes);
      setServerSyncedAt(new Date().toLocaleTimeString("ko-KR"));
      emit(`실서버 동기화 완료 (SOAP + 검사오더 ${examRes.length}건)`);
    } catch (e: any) {
      emit(`실서버 동기화 실패: ${e?.message || e}`);
    }
  };

  // [REMOVED] serverSyncEnabled useEffect 제거 — 체크박스 자동 동기화 불필요
  // 동기화는 "동기화 실행" 버튼 클릭으로만 수행

  return (
    <RoleGate allowed={["DOC", "SYS"]}>
      <div className="page-grid page-grid--readable">
        <GlassCard title="진료" subtitle="SOAP + 검사/영상(복수 선택) · 의사/시스템관리자 전용">
          <div className="form-grid tri">
            <div className="inline-check-group" style={{ gridColumn: "1 / -1" }}>
              {/* [MODIFIED] 체크박스 2개 → 동기화 버튼 1개 */}
              {/* 실서버 저장: 세션 accessToken 존재 시 자동 활성화 */}
              <button type="button" onClick={() => void syncClinicalFromServer()} disabled={syncLoading}>동기화 실행</button>
              {serverWriteEnabled && <small className="muted">실서버 저장 모드 활성</small>}
              {serverSyncedAt && <small className="muted">최근 동기화: {serverSyncedAt}</small>}
            </div>
            <label>
              <span>접수 선택</span>
              <select value={visitId} onChange={(e) => setVisitId(Number(e.target.value))}>
                {activeVisits.map((v) => {
                  const p = patientsById[v.patientId];
                  // [FIXED B3] 이름 우선순위: 로컬 patients → 서버 patientName → "환자"
                  const displayName = p?.name ?? v.patientName ?? "환자";
                  return (
                    <option key={v.id} value={v.id}>
                      {v.id} / {displayName} / {v.status}
                    </option>
                  );
                })}
              </select>
            </label>
            <div className="info-pill">
              <span>환자 정보</span>
              <strong>{patient ? `${patient.name} · ${patient.gender === "M" ? "남(M)" : "여(F)"} · ${formatRrnMasked(patient.rrnFront, patient.rrnBack)}` : "선택 없음"}</strong>
              <small>상태: {visit ? STATUS_LABEL[visit.status] : "-"}</small>
            </div>
            <div className="info-pill">
              <span>연동 계약(설계안 → 현재 백엔드)</span>
              <strong>SOAP: /clinical/emr/soaps/{'{visitId}'} → 현재 /emr/soaps/{'{visitId}'}</strong>
              <small>Orders: /clinical/orders → 현재 /orders (adapter 경유 권장)</small>
            </div>
          </div>

          <div className="split-grid">
            <GlassCard title="SOAP 입력" className="nested-card soap-panel-card">
              <div className="soap-grid soap-grid--spaced">
                <label><span>S (Subjective, 환자 증세)</span><textarea value={soap.subjective} onChange={(e) => setSoap(s => ({ ...s, subjective: e.target.value }))} /></label>
                <label><span>O (Objective, 의사 소견)</span><textarea value={soap.objective} onChange={(e) => setSoap(s => ({ ...s, objective: e.target.value }))} /></label>
                <label><span>A (Assessment, 평가)</span><textarea value={soap.assessment} onChange={(e) => setSoap(s => ({ ...s, assessment: e.target.value }))} /></label>
                <label><span>P (Plan, 진료계획)</span><textarea value={soap.plan} onChange={(e) => setSoap(s => ({ ...s, plan: e.target.value }))} /></label>
              </div>
              <div className="button-row soap-save-row">
                {/* [MODIFIED] state.session은 UserSession | null → ?? undefined로 HeadersInput 타입 호환 */}
                <button className="primary-btn" type="button" onClick={async () => { try { if (serverWriteEnabled) await saveSoapServer({ session: state.session ?? undefined ?? undefined, visitId, soap }); emit(saveSoap(visitId, soap).message + (serverWriteEnabled ? " (서버 저장 포함)" : "")); } catch (e: any) { emit(`SOAP 서버 저장 실패: ${e?.message || e}`); } }}>SOAP 저장</button>
              </div>
            </GlassCard>

            <GlassCard title="기본검사 / 영상 / 내시경검사 오더" subtitle="'없음'은 같은 그룹 내 상호배타" className="nested-card">
              <div className="order-check-grid">
                {(["LAB", "RAD", "PROC"] as ExamCategory[]).map((category) => (
                  <div key={category} className="check-panel">
                    <h4>{category === "LAB" ? "기본검사(LAB)" : category === "RAD" ? "영상(RAD)" : "내시경검사(PROC)"}</h4>
                    <div className="check-list">
                      {EXAM_OPTIONS[category].map((item) => {
                        const checked = selectedItems.some((s) => s.code === item.code);
                        return (
                          <label key={item.code} className={`check-item ${checked ? "is-checked" : ""}`}>
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={() => toggleItem(category, item)}
                            />
                            <span>{item.name}</span>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>

              <div className="order-selected-list order-selected-list--spaced">
                <strong>선택 결과</strong>
                <ul>
                  {selectedItems.map((i) => <li key={i.code}>{i.category} · {i.name}</li>)}
                  {selectedItems.length === 0 && <li className="muted">선택 없음</li>}
                </ul>
              </div>

              <div className="button-row soap-save-row">
                {/* [MODIFIED] state.session은 UserSession | null → ?? undefined로 HeadersInput 타입 호환 */}
                <button className="primary-btn" type="button" onClick={async () => { try { if (serverWriteEnabled) await saveExamOrdersByVisitServer({ session: state.session ?? undefined ?? undefined, visitId, items: selectedItems }); emit(saveExamOrders(visitId, selectedItems).message + (serverWriteEnabled ? " (서버 저장 포함)" : "")); } catch (e: any) { emit(`검사오더 서버 저장 실패: ${e?.message || e}`); } }}>
                  검사/영상 오더 저장
                </button>
              </div>
            </GlassCard>
          </div>

          {message && <div className="toast-mini">{message}</div>}
        </GlassCard>
      </div>
    </RoleGate>
  );
}
