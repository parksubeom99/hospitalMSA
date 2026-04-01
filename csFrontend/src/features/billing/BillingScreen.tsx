"use client";

import { useEffect, useMemo, useState } from "react";
import { GlassCard } from "@/shared/components/GlassCard";
import { RoleGate } from "@/shared/components/RoleGate";
import { useHospital } from "@/shared/store/HospitalStore";
import { formatCurrency } from "@/shared/lib/format";
import { STATUS_LABEL } from "@/shared/config/constants";
import { buildInvoiceItems, totalAmount } from "@/shared/lib/price";
import { createInvoiceServer, payInvoiceServer, type BackendInvoice } from "@/shared/services/billingApi";
import { useInvoicesQuery } from "@/shared/services/billing/billingQueries"; // [ADDED]

function backendInvoiceStatusLabel(status?: string) {
  const x = String(status || "").toUpperCase();
  if (x === "PAID") return "PAID";
  if (x === "CANCELED" || x === "CANCELLED") return "CANCELED";
  if (x === "ISSUED") return "UNPAID";
  return x || "-";
}

export function BillingScreen() {
  const { state, patientsById, generateInvoiceFromFinalOrder, payInvoice, updateVisitStatus } = useHospital();
  const [visitId, setVisitId] = useState<number>(() => {
    const withFinalOrder = Object.keys(state.finalOrders).map(Number);
    return withFinalOrder[0] ?? state.visits[0]?.id ?? 0;
  });
  const [message, setMessage] = useState("");
  // [MODIFIED] 체크박스 2개 → 동기화 버튼 1개로 단순화
  // 실서버 저장/결제: 세션 accessToken 존재 시 자동 활성화
  const serverWriteEnabled = !!state.session?.accessToken;
  // [MODIFIED] useState(syncLoading/serverSyncedAt/serverInvoices) → React Query로 대체
  const invoicesQuery = useInvoicesQuery({ visitId }); // [ADDED]
  const syncLoading = invoicesQuery.isFetching; // [MODIFIED]
  const [serverSyncedAt, setServerSyncedAt] = useState<string | null>(null);
  const serverInvoices: BackendInvoice[] = invoicesQuery.data ?? []; // [MODIFIED]
  const [lastServerPayment, setLastServerPayment] = useState<{ paymentId: number; method: string; paidAt?: string } | null>(null);

  const targetVisit = state.visits.find((v) => v.id === visitId);
  const patient = targetVisit ? patientsById[targetVisit.patientId] : undefined;

  const latestInvoice = useMemo(() => {
    return state.invoices
      .filter((i) => i.visitId === visitId)
      .sort((a, b) => b.invoiceId - a.invoiceId)[0];
  }, [state.invoices, visitId]);

  const latestServerInvoice = useMemo(() => {
    return serverInvoices
      .filter((i) => i.visitId === visitId)
      .sort((a, b) => b.invoiceId - a.invoiceId)[0];
  }, [serverInvoices, visitId]);

  const finalOrder = state.finalOrders[visitId];

  const emit = (msg: string) => {
    setMessage(msg);
    window.setTimeout(() => setMessage(""), 2400);
  };

  // [MODIFIED] 직접 fetch → React Query refetch() 위임
  const syncBillingFromServer = async () => {
    if (!state.session?.accessToken) return emit("실서버 IAM 로그인 후 동기화 가능합니다.");
    if (!visitId) return emit("접수를 먼저 선택해주세요.");
    try {
      const result = await invoicesQuery.refetch(); // [MODIFIED]
      const list = result.data ?? [];
      setServerSyncedAt(new Date().toLocaleTimeString("ko-KR"));
      emit(`실서버 수납 동기화 완료 (청구 ${list.length}건)`);
    } catch (e: any) {
      emit(`실서버 수납 동기화 실패: ${e?.message || e}`);
    }
  };

  // [REMOVED] serverSyncEnabled useEffect 제거 — 버튼 클릭으로만 동기화

  const handleGenerateInvoice = async () => {
    try {
      if (!visitId) return emit("접수를 먼저 선택해주세요.");
      const localRes = generateInvoiceFromFinalOrder(visitId);
      if (!localRes.ok) return emit(localRes.message);

      if (!serverWriteEnabled) return emit(localRes.message);
      if (!state.session?.accessToken) return emit("실서버 저장 모드는 IAM 로그인 후 사용 가능합니다.");
      if (!finalOrder) return emit("최종오더가 없어 실서버 청구 생성이 불가합니다.");

      const amount = totalAmount(buildInvoiceItems(finalOrder));
      await createInvoiceServer({ session: state.session ?? undefined, visitId, amount });
      await syncBillingFromServer();
      emit(`${localRes.message} (실서버 청구 생성 포함)`);
    } catch (e: any) {
      emit(`영수증 서버 생성 실패: ${e?.message || e}`);
    }
  };

  const handlePay = async (method: "CARD" | "CASH") => {
    try {
      if (!latestInvoice && !latestServerInvoice) return emit("결제할 영수증이 없습니다.");

      if (!serverWriteEnabled) {
        if (!latestInvoice) return emit("로컬 영수증이 없습니다. 먼저 영수증 자동 생성을 눌러주세요.");
        emit(payInvoice(latestInvoice.invoiceId, method).message);
        return;
      }

      if (!state.session?.accessToken) return emit("실서버 저장 모드는 IAM 로그인 후 사용 가능합니다.");

      // [FIX] 실서버 Invoice가 없으면 먼저 동기화 후 재확인
      if (!latestServerInvoice) {
        emit("실서버 영수증을 조회 중...");
        await syncBillingFromServer();
        return; // 동기화 후 재렌더링되면 다시 버튼 클릭
      }

      const target = latestServerInvoice;
      // [FIX] invoiceId 유효성 검증
      if (!target.invoiceId || target.invoiceId <= 0) {
        return emit(`실서버 영수증 ID가 유효하지 않습니다. invoiceId=${target.invoiceId}`);
      }
      if (String(target.status).toUpperCase() === "PAID") return emit("이미 결제 완료된 영수증입니다.");

      const payment = await payInvoiceServer({
        session: state.session ?? undefined,
        invoiceId: target.invoiceId,
        method,
        amount: target.totalAmount,
        idempotencyKey: `front-step4-pay-${target.invoiceId}-${method}`,
      });
      setLastServerPayment({ paymentId: payment.paymentId, method: payment.method, paidAt: payment.paidAt });

      // 로컬 UI도 즉시 반영(다음 단계 전 화면 안정성 유지)
      if (latestInvoice && latestInvoice.status !== "PAID") {
        payInvoice(latestInvoice.invoiceId, method);
      } else {
        updateVisitStatus(visitId, "COMPLETED");
      }

      await syncBillingFromServer();
      emit("실서버 결제 완료 + 방문상태 완료 반영(서버/로컬)");
    } catch (e: any) {
      emit(`결제 서버 처리 실패: ${e?.message || e}`);
    }
  };

  return (
    <RoleGate allowed={["ADMIN", "SYS"]}>
      <div className="page-grid page-grid--readable">
        <GlassCard title="수납" subtitle="최종오더 결과 기반 영수증 자동 산출 / 결제 처리">
          <div className="form-grid tri">
            <div className="inline-check-group" style={{ gridColumn: "1 / -1" }}>
              {/* [MODIFIED] 체크박스 2개 → 동기화 버튼 1개 */}
              <button type="button" onClick={() => void syncBillingFromServer()} disabled={syncLoading}>동기화 실행</button>
              {serverWriteEnabled && <small className="muted">실서버 저장/결제 모드 활성</small>}
              {serverSyncedAt && <small className="muted">최근 동기화: {serverSyncedAt}</small>}
            </div>

            <label>
              <span>접수 선택</span>
              <select value={visitId} onChange={(e) => setVisitId(Number(e.target.value))}>
                {state.visits.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.id} / {patientsById[v.patientId]?.name ?? "-"} / {STATUS_LABEL[v.status]}
                  </option>
                ))}
              </select>
            </label>
            <div className="info-pill">
              <span>환자</span>
              <strong>{patient ? patient.name : "선택 없음"}</strong>
              <small>결제 후 규칙: 수납 완료 시 진료완료 처리</small>
            </div>
            <div className="button-row">
              <button type="button" className="primary-btn" onClick={() => void handleGenerateInvoice()}>
                영수증 자동 생성
              </button>
            </div>
          </div>

          <div className="split-grid">
            <GlassCard title="최종오더 요약" className="nested-card">
              {!finalOrder ? (
                <div className="empty-state muted">최종오더가 없습니다.</div>
              ) : (
                <div className="summary-box">
                  <div className="summary-row"><span>유형</span><strong>{finalOrder.types.join(", ") || "-"}</strong></div>
                  {finalOrder.medications.length > 0 && (
                    <div className="summary-row">
                      <span>약제</span>
                      <strong>{finalOrder.medications.map((m) => `${m.drugName} x${m.qty}`).join(", ")}</strong>
                    </div>
                  )}
                  {(finalOrder.injections ?? []).length > 0 && (
                    <div className="summary-row">
                      <span>주사</span>
                      <strong>{(finalOrder.injections ?? []).map((m) => m.injectionName).join(", ")}</strong>
                    </div>
                  )}
                  {finalOrder.surgery && (
                    <div className="summary-row">
                      <span>수술</span>
                      <strong>{finalOrder.surgery.surgeryType === "INTERNAL" ? "내과수술" : "외과수술"} / {finalOrder.surgery.roomNo}번실</strong>
                    </div>
                  )}
                  {finalOrder.admission && (
                    <div className="summary-row">
                      <span>입원</span>
                      <strong>{finalOrder.admission.wardNo}번 병동 / {finalOrder.admission.nights}박</strong>
                    </div>
                  )}
                </div>
              )}
            </GlassCard>

            <GlassCard title="영수증 미리보기" subtitle="약 1,000 / 주사 5,000 / 내과수술 50,000 / 외과수술 100,000 / 입원 1박 10,000" className="nested-card">
              {!latestInvoice && !latestServerInvoice ? (
                <div className="empty-state muted">영수증이 아직 생성되지 않았습니다.</div>
              ) : (
                <>
                  {latestServerInvoice && (
                    <div className="info-pill" style={{ marginBottom: 10 }}>
                      <span>실서버 청구</span>
                      <strong>#{latestServerInvoice.invoiceId} / {backendInvoiceStatusLabel(latestServerInvoice.status)}</strong>
                      <small>총액 {formatCurrency(latestServerInvoice.totalAmount)} {latestServerInvoice.items?.length ? `· 항목 ${latestServerInvoice.items.length}건` : "· TOTAL 라인 중심"}</small>
                    </div>
                  )}

                  <div className="table-wrap">
                    <table className="ui-table compact">
                      <thead>
                        <tr>
                          <th>항목</th>
                          <th>수량</th>
                          <th>단가</th>
                          <th>금액</th>
                          <th>비고</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(latestInvoice?.items ?? []).map((item, idx) => (
                          <tr key={`local-${idx}`}>
                            <td>{item.itemName}</td>
                            <td>{item.qty}</td>
                            <td>{formatCurrency(item.unitPrice)}</td>
                            <td>{formatCurrency(item.amount)}</td>
                            <td>{item.metaLabel ?? "-"}</td>
                          </tr>
                        ))}

                        {!latestInvoice && latestServerInvoice?.items?.length ? latestServerInvoice.items.map((item, idx) => (
                          <tr key={`server-${idx}`}>
                            <td>{item.itemName ?? item.itemCode ?? "TOTAL"}</td>
                            <td>{item.qty ?? 1}</td>
                            <td>{formatCurrency(Number(item.unitPrice ?? 0))}</td>
                            <td>{formatCurrency(Number(item.lineTotal ?? item.unitPrice ?? 0))}</td>
                            <td>{item.itemCode ?? "-"}</td>
                          </tr>
                        )) : null}

                        {!latestInvoice && (!latestServerInvoice?.items || latestServerInvoice.items.length === 0) && latestServerInvoice ? (
                          <tr>
                            <td>Total Amount</td>
                            <td>1</td>
                            <td>{formatCurrency(latestServerInvoice.totalAmount)}</td>
                            <td>{formatCurrency(latestServerInvoice.totalAmount)}</td>
                            <td>TOTAL</td>
                          </tr>
                        ) : null}
                      </tbody>
                    </table>
                  </div>

                  <div className="receipt-footer">
                    <div>
                      <strong>로컬 상태</strong>: {latestInvoice ? latestInvoice.status : "-"}
                      {latestServerInvoice && <> · <strong>실서버 상태</strong>: {backendInvoiceStatusLabel(latestServerInvoice.status)}</>}
                    </div>
                    <div className="receipt-total">{formatCurrency(latestInvoice?.totalAmount ?? latestServerInvoice?.totalAmount ?? 0)}</div>
                  </div>

                  {lastServerPayment && (
                    <div className="muted" style={{ marginBottom: 8 }}>
                      최근 서버결제: #{lastServerPayment.paymentId} / {lastServerPayment.method} {lastServerPayment.paidAt ? `/ ${lastServerPayment.paidAt}` : ""}
                    </div>
                  )}

                  <div className="button-row">
                    <button type="button" onClick={() => void handlePay("CARD")} disabled={(serverWriteEnabled ? String(latestServerInvoice?.status || "").toUpperCase() === "PAID" : latestInvoice?.status === "PAID")}>
                      카드 결제
                    </button>
                    <button type="button" onClick={() => void handlePay("CASH")} disabled={(serverWriteEnabled ? String(latestServerInvoice?.status || "").toUpperCase() === "PAID" : latestInvoice?.status === "PAID")}>
                      현금 결제
                    </button>
                  </div>
                </>
              )}
            </GlassCard>
          </div>

          {message && <div className="toast-mini">{message}</div>}
        </GlassCard>
      </div>
    </RoleGate>
  );
}
