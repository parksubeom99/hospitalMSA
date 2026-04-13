"use client";

import { useMemo, useState } from "react";
import { GlassCard } from "@/shared/components/GlassCard";
import { Donut3D } from "@/shared/components/Donut3D";
import { StatusBadge } from "@/shared/components/StatusBadge";
import { useHospital } from "@/shared/store/HospitalStore";
import { maskName, maskReservationName, maskPhone, formatRrnMasked } from "@/shared/lib/masking";
import { formatDateTime } from "@/shared/lib/date";
import { STATUS_LABEL } from "@/shared/config/constants";
import { useDashboardSummaryQuery } from "@/shared/services/dashboard/dashboardQueries";

export function DashboardScreen() {
  const { state, capacity, patientsById } = useHospital();

  const [useServerSummary, setUseServerSummary] = useState(false);
  const [autoSyncServer, setAutoSyncServer] = useState(false);

  const dashboardSummaryQuery = useDashboardSummaryQuery({
    enabled: useServerSummary,
    refetchInterval: useServerSummary && autoSyncServer ? 30_000 : false,
  });

  const serverSummary = dashboardSummaryQuery.data ?? null;
  const syncing = dashboardSummaryQuery.isFetching;
  const syncError = dashboardSummaryQuery.error instanceof Error ? dashboardSummaryQuery.error.message : "";

  const localReceptionRows = state.visits
    .slice()
    .sort((a, b) => b.id - a.id)
    .slice(0, 10)
    .map((visit) => {
      const patient = patientsById[visit.patientId];
      return {
        visitId: visit.id,
        patientNameMasked: patient ? maskName(patient.name) : "-",
        genderLabel: patient ? (patient.gender === "M" ? "남(M)" : "여(F)") : "-",
        rrnMasked: patient ? formatRrnMasked(patient.rrnFront, patient.rrnBack) : "-",
        status: STATUS_LABEL[visit.status],
        registeredAt: visit.registeredAt,
      };
    });

  const localReservationRows = state.reservations
    .filter((r) => r.status === "RESERVED")
    .slice()
    .sort((a, b) => a.reservedAt.localeCompare(b.reservedAt))
    .slice(0, 10)
    .map((r) => {
      const patient = patientsById[r.patientId];
      return {
        reservationId: r.id,
        reservedAt: r.reservedAt,
        nameMasked: patient ? maskReservationName(patient.name) : "-",
        phoneMasked: patient ? maskPhone(patient.phone) : "-",
      };
    });

  const syncServerSummary = async () => {
    await dashboardSummaryQuery.refetch();
  };

  const serverBackedCapacity = useMemo(() => {
    if (!serverSummary) return null;
    const waitingAndInTreatment = Math.max(0, Number(serverSummary.counts.waiting || 0));
    const reservation = Math.max(0, Number(serverSummary.counts.reservation || 0));
    const emergency = Math.max(0, Number(serverSummary.counts.emergency || 0));
    const current = waitingAndInTreatment + reservation + emergency;
    const max = capacity.max;
    const level = (current >= max ? "FULL" : current >= 25 ? "DANGER" : current >= 20 ? "WARN" : "SAFE") as import("@/shared/types/domain").CapacityLevel; // [MODIFIED] string → CapacityLevel 타입 단언
    return {
      ...capacity,
      waitingAndInTreatment,
      reservation,
      emergency,
      current,
      max,
      level,
      canRegister: current < max,
    };
  }, [serverSummary, capacity]);

  const viewCapacity = useServerSummary && serverBackedCapacity ? serverBackedCapacity : capacity;

  const receptionRows = useMemo(() => {
    if (!(useServerSummary && serverSummary)) return localReceptionRows;
    return serverSummary.patients.slice(0, 10).map((p) => ({
      visitId: p.visitId,
      patientNameMasked: p.patientName ? maskName(p.patientName) : "-",
      genderLabel: "-",
      rrnMasked: "******-*******",
      status: STATUS_LABEL[String(p.status || "").toUpperCase()] || String(p.status || "-") || "-",
      registeredAt: p.createdAt || "",
    }));
  }, [useServerSummary, serverSummary, localReceptionRows]);

  // 현재 백엔드 summary에는 예약 테이블 상세 목록이 없으므로 로컬 예약 목록 유지 (카운트만 summary 사용 가능)
  const reservationRows = localReservationRows;

  const levelTone = viewCapacity.level === "SAFE" ? "green" : viewCapacity.level === "WARN" ? "orange" : "red";

  return (
    <div className="page-grid">
      <GlassCard
        title="대시보드"
        subtitle="환자 접수 현황 / 예약 현황은 조회 전용"
        right={<StatusBadge label={`${viewCapacity.level} · ${viewCapacity.canRegister ? "등록 가능" : "등록 차단"}`} tone={levelTone as any} />}
      >
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 10 }}>
          <button className="btn ghost" type="button" onClick={() => { setUseServerSummary(true); setAutoSyncServer(true); void syncServerSummary(); }} disabled={syncing}>
            {syncing ? "동기화 중..." : "서버 동기화"}
          </button>
          {serverSummary?.generatedAt && (
            <span className="helper-text">서버 집계시각: {formatDateTime(serverSummary.generatedAt)}</span>
          )}
          {syncError && <span className="helper-text" style={{ color: "#ff9aa5" }}>{syncError}</span>}
        </div>

        <div className="dashboard-grid">
          <div>
            <Donut3D
              waitingAndInTreatment={viewCapacity.waitingAndInTreatment}
              reservation={viewCapacity.reservation}
              emergency={viewCapacity.emergency}
              current={viewCapacity.current}
              max={viewCapacity.max}
              level={viewCapacity.level}
            />
            <div className={`capacity-banner ${String(viewCapacity.level).toLowerCase()}`}>
              <div>
                <strong>운영 제한 규칙</strong>
                <p>총 인원 = 대기 + 진료중 + 예약 + 응급 (최대 30명)</p>
              </div>
              <div className="capacity-banner__count">{viewCapacity.current} / 30</div>
            </div>
          </div>

          <div className="stack-col">
            <GlassCard title="환자 접수 현황" subtitle={useServerSummary ? "실서버 summary 기반 (민감정보 마스킹 고정)" : "대시보드에서는 등록/수정 불가"} className="nested-card">
              <div className="table-wrap">
                <table className="ui-table compact">
                  <thead>
                    <tr>
                      <th>접수번호</th>
                      <th>환자명</th>
                      <th>성별</th>
                      <th>주민번호</th>
                      <th>상태</th>
                      <th>등록시각</th>
                    </tr>
                  </thead>
                  <tbody>
                    {receptionRows.map((r) => (
                      <tr key={r.visitId}>
                        <td>{r.visitId}</td>
                        <td>{r.patientNameMasked}</td>
                        <td>{r.genderLabel}</td>
                        <td>{r.rrnMasked}</td>
                        <td>{r.status}</td>
                        <td>{r.registeredAt ? formatDateTime(r.registeredAt) : "-"}</td>
                      </tr>
                    ))}
                    {receptionRows.length === 0 && (
                      <tr><td colSpan={6} className="empty-cell">데이터 없음</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </GlassCard>

            <GlassCard title="예약 현황" subtitle={useServerSummary ? "목록은 로컬 표시, 카운트는 서버 집계 반영 가능" : "예약자명/전화번호 마스킹 표시"} className="nested-card">
              <div className="table-wrap">
                <table className="ui-table compact">
                  <thead>
                    <tr>
                      <th>예약번호</th>
                      <th>예약시각</th>
                      <th>예약자</th>
                      <th>전화번호</th>
                      <th>상태</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reservationRows.map((r) => (
                      <tr key={r.reservationId}>
                        <td>{r.reservationId}</td>
                        <td>{formatDateTime(r.reservedAt)}</td>
                        <td>{r.nameMasked}</td>
                        <td>{r.phoneMasked}</td>
                        <td>예약</td>
                      </tr>
                    ))}
                    {reservationRows.length === 0 && (
                      <tr><td colSpan={5} className="empty-cell">데이터 없음</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </GlassCard>
          </div>
        </div>
      </GlassCard>
    </div>
  );
}
