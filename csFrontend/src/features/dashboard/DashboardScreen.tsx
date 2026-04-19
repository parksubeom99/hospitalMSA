"use client";

import { useMemo } from "react";
import { GlassCard } from "@/shared/components/GlassCard";
import { Donut3D } from "@/shared/components/Donut3D";
import { StatusBadge } from "@/shared/components/StatusBadge";
import { useHospital } from "@/shared/store/HospitalStore";
import { maskName, maskReservationName, maskPhone, formatRrnMasked } from "@/shared/lib/masking";
import { formatDateTime } from "@/shared/lib/date";
import { STATUS_LABEL } from "@/shared/config/constants";
import { useDashboardSummaryQuery } from "@/shared/services/dashboard/dashboardQueries";
import { fetchEmergencyCount } from "@/shared/services/dashboard/dashboardApi";
import { useReservationsQuery } from "@/shared/services/reception/receptionQueries"; // [P1] 서버 예약 데이터

function DashboardContent() {
  const { state, capacity, patientsById, setEmergencyCount } = useHospital();

  // authSource === "server" = IAM 실로그인으로 발급된 유효한 JWT 보유 상태
  // 데모 세션도 accessToken(데모용)이 존재하므로 !!accessToken 으로 판단하면 안 됨
  const isServerSession = state.session?.authSource === "server";
  // [ADDED] 로그인 여부 (데모/서버 무관)
  const isLoggedIn = !!state.session;

  const dashboardSummaryQuery = useDashboardSummaryQuery({
    enabled: isServerSession,
    refetchInterval: false,
  });

  const serverSummary = dashboardSummaryQuery.data ?? null;
  const syncing = dashboardSummaryQuery.isFetching;
  const syncError = dashboardSummaryQuery.error instanceof Error
    ? `실서버 집계 오류: ${dashboardSummaryQuery.error.message}`
    : "";

  // 로컬 접수 목록 (항상 준비 — 서버 데이터 없을 때 fallback)
  const localReceptionRows = useMemo(() =>
    state.visits
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
      }),
    [state.visits, patientsById]
  );

  const localReservationRows = useMemo(() =>
    state.reservations
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
      }),
    [state.reservations, patientsById]
  );

  const syncServerSummary = async () => {
    // 대기/예약: 서버 집계 refetch + 예약 목록 refetch
    await Promise.all([
      dashboardSummaryQuery.refetch(),
      reservationsQuery.refetch(), // [P1] 예약현황 동기화
    ]);
    // 응급: admin_config 서버값으로 로컬 동기화
    try {
      const serverEmergency = await fetchEmergencyCount();
      setEmergencyCount(serverEmergency);
    } catch {
      // 서버 응급값 조회 실패 시 로컬값 유지
    }
  };

  const serverBackedCapacity = useMemo(() => {
    if (!serverSummary) return null;
    const waitingAndInTreatment = Math.max(0, Number(serverSummary.counts.waiting || 0));
    const reservation = Math.max(0, Number(serverSummary.counts.reservation || 0));
    // [FIXED] 응급은 admin_config 서버 설정값 사용 (visit 집계 아님)
    //         동기화 시 fetchEmergencyCount()로 서버값 반영, 없으면 로컬 emergencyCount 유지
    const emergency = Math.max(0, state.emergencyCount);
    const current = waitingAndInTreatment + reservation + emergency;
    const max = capacity.max;
    const level = (current >= max ? "FULL" : current >= 25 ? "DANGER" : current >= 20 ? "WARN" : "SAFE") as import("@/shared/types/domain").CapacityLevel;
    return { ...capacity, waitingAndInTreatment, reservation, emergency, current, max, level, canRegister: current < max };
  }, [serverSummary, capacity]);

  const viewCapacity = serverBackedCapacity ?? capacity;
  const isServerData = !!serverSummary;

  const receptionRows = useMemo(() => {
    if (!isServerData) return localReceptionRows;
    return serverSummary!.patients.slice(0, 10).map((p) => ({
      visitId: p.visitId,
      patientNameMasked: p.patientName ? maskName(p.patientName) : "-",
      genderLabel: "-",
      rrnMasked: "******-*******",
      status: STATUS_LABEL[String(p.status || "").toUpperCase()] || String(p.status || "-") || "-",
      registeredAt: p.createdAt || "",
    }));
  }, [isServerData, serverSummary, localReceptionRows]);

  // [FIXED P1] 예약현황 — 서버 데이터 우선, 로컬 fallback
  // 이전: localReservationRows(시드 데이터) 하드코딩 → 서버 예약과 불일치
  // 수정: isServerSession 시 admin/reservations API 데이터 사용
  // [FIXED v3.1] length > 0 조건 제거 — 서버 0건도 서버 데이터로 취급 (로컬 유령 차단)
  const reservationsQuery = useReservationsQuery({ enabled: isServerSession });
  const isServerReservation = isServerSession
    && Array.isArray(reservationsQuery.data);
  const reservationRows = isServerReservation
    ? reservationsQuery.data!
        .filter((r) => r.status === "RESERVED")
        .sort((a, b) => a.reservedAt.localeCompare(b.reservedAt))
        .slice(0, 10)
        .map((r) => ({
          reservationId: r.id,
          reservedAt: r.reservedAt,
          nameMasked: r.contactName ? r.contactName.slice(0, 1) + "**" : "-",
          phoneMasked: r.contactPhone ? r.contactPhone.slice(0, 7) + "-****" : "-",
        }))
    : localReservationRows;
  const levelTone = viewCapacity.level === "SAFE" ? "green" : viewCapacity.level === "WARN" ? "orange" : "red";

  // [MODIFIED] 상태 텍스트 — 비로그인/데모/서버 3단계 명확 구분
  const statusText = !isLoggedIn
    ? "로그인 후 실서버 집계를 사용할 수 있습니다"
    : !isServerSession
    ? "로컬 집계 사용 중 (실서버 집계는 IAM 로그인 후 사용 가능)"
    : syncing
    ? "집계 조회 중..."
    : isServerData
    ? "실서버 집계 사용 중"
    : "로컬 집계 사용 중";

  return (
    <div className="page-grid">
      <GlassCard
        title="대시보드"
        subtitle="환자 접수 현황 / 예약 현황은 조회 전용"
        right={<StatusBadge label={`${viewCapacity.level} · ${viewCapacity.canRegister ? "등록 가능" : "등록 차단"}`} tone={levelTone as any} />}
      >
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 10, alignItems: "center" }}>
          {/* [MODIFIED] 비로그인 시 안내 문구 강조 표시 */}
          <span className={!isLoggedIn ? "helper-text" : "inline-muted"} style={!isLoggedIn ? { color: "#7ecfff" } : undefined}>
            {statusText}
          </span>
          {isServerSession && (
            <button className="btn ghost" type="button" onClick={() => void syncServerSummary()} disabled={syncing}>
              {syncing ? "동기화 중..." : "집계 동기화"}
            </button>
          )}
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
            <GlassCard
              title="환자 접수 현황"
              subtitle={isServerData ? "실서버 summary 기반 (민감정보 마스킹 고정)" : "대시보드에서는 등록/수정 불가"}
              className="nested-card"
            >
              <div className="table-wrap">
                <table className="ui-table compact">
                  <thead>
                    <tr>
                      <th>접수번호</th><th>환자명</th><th>성별</th><th>주민번호</th><th>상태</th><th>등록시각</th>
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

            <GlassCard
              title="예약 현황"
              subtitle={isServerData ? "목록은 로컬 표시, 카운트는 서버 집계 반영 가능" : "예약자명/전화번호 마스킹 표시"}
              className="nested-card"
            >
              <div className="table-wrap">
                <table className="ui-table compact">
                  <thead>
                    <tr>
                      <th>예약번호</th><th>예약시각</th><th>예약자</th><th>전화번호</th><th>상태</th>
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

export function DashboardScreen() {
  const { hydrated } = useHospital();
  if (!hydrated) return null;
  return <DashboardContent />;
}
