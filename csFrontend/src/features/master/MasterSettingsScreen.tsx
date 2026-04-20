"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { GlassCard } from "@/shared/components/GlassCard";
import { RoleGate } from "@/shared/components/RoleGate";
import { useHospital } from "@/shared/store/HospitalStore";
import type { StaffProfile } from "@/shared/types/domain";
import { createMasterStaffServer, deactivateMasterStaffServer, updateMasterStaffServer } from "@/shared/services/masterStaffApi";
import { useMasterStaffQuery, masterQueryKeys } from "@/shared/services/master/masterQueries";
import { useQueryClient } from "@tanstack/react-query";

type EmailDomainOption = "naver.com" | "daum.net" | "google.com" | "custom";

type FormState = StaffProfile & {
  phoneMid: string;
  phoneLast: string;
  emailId: string;
  emailDomainOption: EmailDomainOption;
  emailDomainCustom: string;
};

function splitPhone(phone: string) {
  const d = phone.replace(/\D/g, "");
  const body = d.startsWith("010") ? d.slice(3) : d;
  return { phoneMid: body.slice(0, 4), phoneLast: body.slice(4, 8) };
}

function buildPhone(mid: string, last: string) {
  return `010-${mid.replace(/\D/g, "").slice(0, 4)}-${last.replace(/\D/g, "").slice(0, 4)}`;
}

function parseEmail(email: string) {
  const [emailId = "", domain = ""] = (email || "").split("@");
  const known = ["naver.com", "daum.net", "google.com", "gmail.com"] as const;
  if (known.includes(domain as any)) {
    return { emailId, emailDomainOption: (domain === "gmail.com" ? "google.com" : domain) as EmailDomainOption, emailDomainCustom: "" };
  }
  return { emailId, emailDomainOption: "custom" as const, emailDomainCustom: domain };
}

function toFormState(staff?: StaffProfile): FormState {
  const base: StaffProfile = staff ?? {
    staffId: 0,
    staffName: "",
    jobType: "DOCTOR",
    department: "내과",
    specialty: "",
    phone: "010--",
    email: "",
    active: true,
  };
  const phone = splitPhone(base.phone || "");
  const email = parseEmail(base.email || "");
  const department = base.jobType === "ADMIN" ? "원무과" : (base.department || "내과");
  return { ...base, department, specialty: base.specialty ?? "", ...phone, ...email };
}

function toStaffProfile(form: FormState): StaffProfile {
  const domain = form.emailDomainOption === "custom" ? form.emailDomainCustom.trim() : form.emailDomainOption;
  return {
    staffId: form.staffId,
    staffName: form.staffName,
    jobType: form.jobType,
    department: form.jobType === "ADMIN" ? "원무과" : form.department,
    specialty: "",
    phone: buildPhone(form.phoneMid, form.phoneLast),
    email: form.emailId.trim() && domain ? `${form.emailId.trim()}@${domain}` : "",
    active: form.active,
  };
}

// [ADDED] 실제 UI 로직 컴포넌트 — hydrated=true 이후에만 렌더됨
// 이유: MasterSettingsScreen 최상단에서 state.session 참조 시
//       SSR(null) vs CSR(복원값) 불일치 → #418/#423/#425 발생
//       → hydrated 확인 후 이 컴포넌트를 렌더함으로써 SSR DOM = null 보장
function MasterSettingsContent() {
  const { state, upsertStaff, removeStaff } = useHospital();
  const isServerSession = state.session?.authSource === "server";
  const serverMode = isServerSession;

  const qc = useQueryClient();
  const masterStaffQuery = useMasterStaffQuery();
  const serverStaff: StaffProfile[] = masterStaffQuery.data ?? [];
  const serverStaffIdsRef = useRef<Set<number>>(new Set(serverStaff.map((s) => s.staffId)));
  const syncedRef = useRef<boolean>(masterStaffQuery.isSuccess);
  const [serverBusy, setServerBusy] = useState(false);
  const [serverLastSyncAt, setServerLastSyncAt] = useState<string>("");
  const [jobFilter, setJobFilter] = useState<"ALL" | "DOCTOR" | "ADMIN">("ALL");
  const [form, setForm] = useState<FormState>(toFormState());
  const [message, setMessage] = useState("");
  const phoneMidRef = useRef<HTMLInputElement | null>(null);
  const phoneLastRef = useRef<HTMLInputElement | null>(null);

  const rows = useMemo(() => {
    const source = serverMode ? serverStaff : state.staff;
    return source
      .filter((s) => s.active !== false)
      .filter((s) => (jobFilter === "ALL" ? true : s.jobType === jobFilter));
  }, [serverMode, serverStaff, state.staff, jobFilter]);

  const emit = (msg: string) => {
    setMessage(msg);
    window.setTimeout(() => setMessage(""), 2600);
  };

  const syncFromServer = async () => {
    if (!isServerSession) {
      emit("실서버 동기화는 IAM 실로그인 후 사용해주세요.");
      return;
    }
    setServerBusy(true);
    try {
      const result = await masterStaffQuery.refetch();
      const list = result.data ?? [];
      const activeList = list.filter((x) => x.active !== false);
      serverStaffIdsRef.current = new Set(activeList.map((x) => x.staffId));
      syncedRef.current = true;
      setServerLastSyncAt(new Date().toLocaleTimeString("ko-KR"));
      emit(`직원 프로필 동기화 완료 (${activeList.length}건)`);
    } catch (e) {
      emit(`동기화 실패: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setServerBusy(false);
    }
  };

  useEffect(() => {
    if (!isServerSession) {
      qc.removeQueries({ queryKey: masterQueryKeys.staff() });
      serverStaffIdsRef.current = new Set();
      syncedRef.current = false;
    }
  }, [isServerSession]);

  const setJobType = (jobType: "DOCTOR" | "ADMIN") => {
    setForm((f) => ({
      ...f,
      jobType,
      department: jobType === "ADMIN" ? "원무과" : (f.department && f.department !== "원무과" ? f.department : "내과"),
      specialty: "",
    }));
  };

  const onSubmit = async () => {
    if (!form.staffName.trim()) return emit("성명을 입력해주세요.");
    if (!form.phoneMid || !form.phoneLast) return emit("연락처를 입력해주세요.");
    if (!form.emailId.trim()) return emit("이메일 아이디를 입력해주세요.");
    if (form.emailDomainOption === "custom" && !form.emailDomainCustom.trim()) return emit("이메일 도메인을 입력해주세요.");

    const payload = toStaffProfile(form);

    if (!serverMode) {
      emit(upsertStaff(payload).message);
      setForm(toFormState());
      return;
    }

    if (!isServerSession) {
      emit("실서버 저장은 IAM 실로그인 후 사용해주세요.");
      return;
    }

    if (!syncedRef.current) {
      emit("동기화 중...");
      await syncFromServer();
    }

    setServerBusy(true);
    try {
      const isUpdate = payload.staffId > 0 && serverStaffIdsRef.current.has(payload.staffId);

      const saved = isUpdate
        ? await updateMasterStaffServer(payload)
        : await createMasterStaffServer({ ...payload, staffId: 0 });

      if (!isUpdate && saved.staffId > 0) {
        serverStaffIdsRef.current.add(saved.staffId);
        qc.setQueryData(masterQueryKeys.staff(), (prev: StaffProfile[] | undefined) =>
          prev ? [...prev, saved] : [saved]
        );
      } else if (isUpdate) {
        qc.setQueryData(masterQueryKeys.staff(), (prev: StaffProfile[] | undefined) =>
          prev ? prev.map((s) => s.staffId === saved.staffId ? saved : s) : [saved]
        );
      }

      emit(isUpdate ? "직원 프로필 수정 저장 완료(서버)" : "직원 프로필 신규 등록 완료(서버)");
      setForm(toFormState());
      setServerLastSyncAt(new Date().toLocaleTimeString("ko-KR"));
    } catch (e) {
      emit(`저장 실패: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setServerBusy(false);
    }
  };

  const selectForEdit = (staff: StaffProfile) => setForm(toFormState(staff));

  const onDelete = async (staff: StaffProfile) => {
    if (!serverMode) {
      emit(removeStaff(staff.staffId).message);
      return;
    }
    if (!isServerSession) {
      emit("실서버 삭제는 IAM 실로그인 후 사용해주세요.");
      return;
    }

    if (staff.staffId <= 0) {
      emit("삭제 불가: 유효하지 않은 직원 ID입니다.");
      return;
    }

    setServerBusy(true);
    try {
      await deactivateMasterStaffServer(staff.staffId);
      qc.setQueryData(masterQueryKeys.staff(), (prev: StaffProfile[] | undefined) =>
        prev ? prev.filter((s) => s.staffId !== staff.staffId) : []
      );
      serverStaffIdsRef.current.delete(staff.staffId);
      emit("직원 프로필 비활성화 완료(서버)");
      setServerLastSyncAt(new Date().toLocaleTimeString("ko-KR"));
    } catch (e) {
      emit(`삭제 실패: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setServerBusy(false);
    }
  };

  return (
    <div className="page-grid page-grid--readable">
      <GlassCard title="마스터 설정" subtitle="의사/원무 직원 프로필 조회·등록·수정·삭제 (사진/MinIO는 후속)">
        <div className="button-row" style={{ marginBottom: 12, flexWrap: "wrap" }}>
          <button type="button" onClick={() => void syncFromServer()} disabled={serverBusy}>직원 동기화</button>
          {serverBusy && <span className="inline-muted">동기화/저장 중...</span>}
          {serverMode
            ? <span className="inline-muted">실서버 CRUD 모드 활성</span>
            : <span className="inline-muted">현재 세션: 데모 로그인</span>
          }
          {!!serverLastSyncAt && <span className="inline-muted">최근 동기화: {serverLastSyncAt}</span>}
        </div>
        <div className="split-grid">
          <GlassCard title="프로필 등록/수정" className="nested-card">
            <div className="form-grid tri master-form-grid">
              <label>
                <span>직무</span>
                <select value={form.jobType} onChange={(e) => setJobType(e.target.value as "DOCTOR" | "ADMIN")}>
                  <option value="DOCTOR">의사</option>
                  <option value="ADMIN">원무</option>
                </select>
              </label>
              <label>
                <span>성명</span>
                <input value={form.staffName} onChange={(e) => setForm((f) => ({ ...f, staffName: e.target.value }))} />
              </label>
              <label>
                <span>부서</span>
                {form.jobType === "DOCTOR" ? (
                  <select value={form.department} onChange={(e) => setForm((f) => ({ ...f, department: e.target.value }))}>
                    <option value="내과">내과</option>
                    <option value="외과">외과</option>
                    <option value="영상의학과">영상의학과</option>
                  </select>
                ) : (
                  <input value="원무과" readOnly />
                )}
              </label>

              <label className="master-form-grid__phone">
                <span>연락처</span>
                <div className="phone-split">
                  <input value="010" readOnly />
                  <input
                    ref={phoneMidRef}
                    inputMode="numeric"
                    maxLength={4}
                    value={form.phoneMid}
                    onChange={(e) => {
                      const v = e.target.value.replace(/\D/g, "").slice(0, 4);
                      setForm((f) => ({ ...f, phoneMid: v }));
                      if (v.length >= 4) phoneLastRef.current?.focus();
                    }}
                    placeholder="1234"
                  />
                  <input
                    ref={phoneLastRef}
                    inputMode="numeric"
                    maxLength={4}
                    value={form.phoneLast}
                    onChange={(e) => setForm((f) => ({ ...f, phoneLast: e.target.value.replace(/\D/g, "").slice(0, 4) }))}
                    placeholder="5678"
                  />
                </div>
              </label>

              <label className="field-stack master-form-grid__email">
                <span>이메일</span>
                <div className="email-split">
                  <input value={form.emailId} onChange={(e) => setForm((f) => ({ ...f, emailId: e.target.value.replace(/\s/g, "") }))} placeholder="아이디" />
                  <span>@</span>
                  <select value={form.emailDomainOption} onChange={(e) => setForm((f) => ({ ...f, emailDomainOption: e.target.value as EmailDomainOption }))}>
                    <option value="naver.com">naver.com</option>
                    <option value="daum.net">daum.net</option>
                    <option value="google.com">google.com</option>
                    <option value="custom">직접입력</option>
                  </select>
                </div>
                {form.emailDomainOption === "custom" && (
                  <input className="email-domain-custom" value={form.emailDomainCustom} onChange={(e) => setForm((f) => ({ ...f, emailDomainCustom: e.target.value.replace(/\s/g, "") }))} placeholder="도메인 직접입력 (예: hospital.co.kr)" />
                )}
              </label>
            </div>
            <div className="button-row">
              <button type="button" className="primary-btn" onClick={() => void onSubmit()} disabled={serverBusy}>
                {form.staffId ? "수정 저장" : "신규 등록"}
              </button>
              <button type="button" onClick={() => setForm(toFormState())}>초기화</button>
            </div>
          </GlassCard>

          <GlassCard
            title="프로필 목록"
            subtitle={serverMode ? "실서버 목록 (active=true 직원만 표시)" : "로컬 데모 데이터"}
            className="nested-card"
          >
            <div className="button-row">
              <button type="button" className={jobFilter === "ALL" ? "active-btn" : ""} onClick={() => setJobFilter("ALL")}>전체</button>
              <button type="button" className={jobFilter === "DOCTOR" ? "active-btn" : ""} onClick={() => setJobFilter("DOCTOR")}>의사</button>
              <button type="button" className={jobFilter === "ADMIN" ? "active-btn" : ""} onClick={() => setJobFilter("ADMIN")}>원무</button>
            </div>
            <div className="table-wrap">
              <table className="ui-table compact master-profile-table">
                <thead>
                  <tr>
                    <th>ID</th><th>직무</th><th>성명</th><th>부서</th><th>연락처</th><th>이메일</th><th>관리</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((s) => (
                    <tr key={s.staffId}>
                      <td>{s.staffId}</td>
                      <td>{s.jobType === "DOCTOR" ? "의사" : "원무"}</td>
                      <td>{s.staffName}</td>
                      <td>{s.department}</td>
                      <td>{s.phone}</td>
                      <td>{s.email}</td>
                      <td>
                        <div className="inline-btns">
                          <button type="button" onClick={() => selectForEdit(s)}>수정</button>
                          <button type="button" onClick={() => void onDelete(s)} disabled={serverBusy}>삭제</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {rows.length === 0 && (
                    <tr>
                      <td colSpan={7} className="empty-cell">
                        {serverMode && !syncedRef.current ? "직원 동기화 버튼을 눌러주세요." : "데이터 없음"}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </GlassCard>
        </div>

        {message && <div className="toast-mini">{message}</div>}
      </GlassCard>
    </div>
  );
}

// [MODIFIED] 껍데기 컴포넌트 — hydrated 확인 후 Content 렌더
// 이유: useHospital()의 state.session이 SSR 시점엔 null,
//       CSR localStorage 복원 후엔 값이 있어 DOM 트리 불일치 발생
//       → hydrated=true 이후에만 MasterSettingsContent를 마운트해
//         SSR DOM = null → mismatch 완전 차단
export function MasterSettingsScreen() {
  const { hydrated } = useHospital(); // [MODIFIED] hydrated만 참조 — session 미참조로 mismatch 원천 차단

  // [ADDED] hydration 완료 전 null 반환 — SSR DOM과 일치
  if (!hydrated) return null;

  return (
    <RoleGate allowed={["ADMIN", "SYS"]}>
      <MasterSettingsContent />
    </RoleGate>
  );
}
