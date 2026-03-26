"use client";

import { useEffect, useState } from "react";
import { GlassCard } from "@/shared/components/GlassCard";
import { useHospital } from "@/shared/store/HospitalStore";

export function LoginScreen() {
  const { state, loginWithCredentials, loginWithServerCredentials, bootstrapAuthSession } = useHospital() as any;
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (state.session) return;
    void (async () => {
      const result = await bootstrapAuthSession();
      if (result.ok) setMsg(result.message);
    })();
  }, [bootstrapAuthSession, state.session]);

  const onSubmit = async () => {
    setLoading(true);
    try {
      const trimmedUsername = username.trim();
      const serverResult = await loginWithServerCredentials({ username: trimmedUsername, password });
      if (serverResult?.ok) {
        setMsg(serverResult.message);
        return;
      }

      const serverMsg = String(serverResult?.message ?? "");
      const isNetworkError = /failed to fetch|networkerror|load failed|fetch/i.test(serverMsg);

      // 회장님 요청 반영: 실서버 실패 시(네트워크/인증/응답 오류 포함) 데모 로그인도 자동 시도
      const demoResult = loginWithCredentials({ username: trimmedUsername, password });
      if (demoResult.ok) {
        setMsg(isNetworkError
          ? `[데모 모드] ${demoResult.message} (IAM 연결 실패로 자동 전환)`
          : `[데모 모드] ${demoResult.message} (IAM 로그인 실패로 자동 전환)`);
        return;
      }

      // 둘 다 실패했을 때는 서버/데모 실패 원인을 함께 보여줌
      setMsg(isNetworkError
        ? `IAM 연결 실패(8181) + 데모 로그인 실패: ${demoResult.message}`
        : `${serverMsg || "IAM 로그인 실패"} / 데모 로그인 실패: ${demoResult.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-grid single page-grid--readable">
      <GlassCard title="로그인" subtitle="아이디/비밀번호 입력 후 권한별 로그인 (JWT Redis Refresh 연동 준비)">
        <div className="form-grid">
          <label>
            <span>아이디</span>
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="예: sys123" />
          </label>
          <label>
            <span>비밀번호</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="비밀번호" />
          </label>
        </div>

        <div className="button-row" style={{ justifyContent: "flex-end" }}>
          <button
            type="button"
            className="primary-btn"
            onClick={onSubmit}
            disabled={loading}
            style={{ minWidth: 100, fontWeight: 800, color: "#f7fbff", textShadow: "0 1px 2px rgba(0,0,0,.18)" }}
          >
            {loading ? "로그인 중..." : "로그인"}
          </button>
        </div>

        {msg && <div className="toast-mini" style={{ marginTop: 8 }}>{msg}</div>}
      </GlassCard>
    </div>
  );
}
