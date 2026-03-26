import { API_BASES } from "@/shared/config/api";
import { clearStoredAuthTokens, loadStoredAuthTokens, saveStoredAuthTokens, type StoredAuthTokenSet } from "@/shared/services/tokenStorage";
import type { RoleCode } from "@/shared/types/domain";

const IAM_BASE_URL = API_BASES.iam;

export interface AuthUserDto {
  username: string;
  displayName: string;
  role: RoleCode;
  staffId?: number;
}

export interface AuthLoginResult {
  token: {
    accessToken: string;
    refreshToken?: string;
    tokenType: "Bearer";
  };
  user: AuthUserDto;
}

interface FetchJsonOptions extends RequestInit {
  tokenOverride?: string;
}

async function fetchJson<T>(path: string, options: FetchJsonOptions = {}): Promise<T> {
  const headers = new Headers(options.headers ?? {});
  headers.set("Content-Type", "application/json");
  const token = options.tokenOverride ?? loadStoredAuthTokens()?.accessToken;
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(`${IAM_BASE_URL}${path}`, {
    ...options,
    headers,
    cache: "no-store",
  });
  const text = await res.text();
  const data = text ? safeJsonParse(text) : undefined;
  if (!res.ok) {
    const msg = extractErrorMessage(data) ?? `HTTP ${res.status}`;
    throw new Error(msg);
  }
  return data as T;
}

function safeJsonParse(text: string): unknown {
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

function extractErrorMessage(data: unknown): string | null {
  if (!data || typeof data !== "object") return null;
  const rec = data as Record<string, unknown>;
  const msg = rec.message;
  return typeof msg === "string" ? msg : null;
}

function normalizeRole(raw: unknown): RoleCode {
  const val = String(raw ?? "").toUpperCase();
  if (val.includes("ADMIN")) return "ADMIN";
  if (val.includes("DOC") || val.includes("DOCTOR")) return "DOC";
  return "SYS";
}

function normalizeLoginResponse(raw: unknown, requestedUsername?: string): AuthLoginResult {
  if (!raw || typeof raw !== "object") throw new Error("로그인 응답 형식이 올바르지 않습니다.");
  const rec = raw as Record<string, unknown>;

  const tokenRec = (rec.token && typeof rec.token === "object") ? (rec.token as Record<string, unknown>) : null;
  const userRec = (rec.user && typeof rec.user === "object") ? (rec.user as Record<string, unknown>) : null;
  if (tokenRec && userRec) {
    const accessToken = String(tokenRec.accessToken ?? "");
    const refreshToken = tokenRec.refreshToken ? String(tokenRec.refreshToken) : undefined;
    if (!accessToken) throw new Error("accessToken 누락");
    const user: AuthUserDto = {
      username: String(userRec.username ?? requestedUsername ?? ""),
      displayName: String(userRec.displayName ?? userRec.name ?? "사용자"),
      role: normalizeRole(userRec.role),
      staffId: userRec.staffId == null ? undefined : Number(userRec.staffId),
    };
    const result: AuthLoginResult = {
      token: { accessToken, refreshToken, tokenType: "Bearer" },
      user,
    };
    saveStoredAuthTokens(result.token);
    return result;
  }

  const accessToken = typeof rec.accessToken === "string" ? rec.accessToken : "";
  if (!accessToken) throw new Error("accessToken 누락");
  const roles = Array.isArray(rec.roles) ? rec.roles : [];
  const role = normalizeRole(roles[0] ?? rec.role);
  const loginId = String(rec.loginId ?? requestedUsername ?? "");
  const result: AuthLoginResult = {
    token: { accessToken, tokenType: "Bearer" },
    user: {
      username: loginId,
      displayName: loginId,
      role,
      staffId: rec.staffId == null || rec.staffId === "" ? undefined : Number(rec.staffId),
    },
  };
  saveStoredAuthTokens(result.token);
  return result;
}

function normalizeMeResponse(raw: unknown): AuthUserDto {
  if (!raw || typeof raw !== "object") throw new Error("사용자 정보 응답 형식이 올바르지 않습니다.");
  const rec = raw as Record<string, unknown>;
  const username = String(rec.username ?? rec.loginId ?? rec.userId ?? "");
  const displayName = String(rec.displayName ?? rec.name ?? rec.username ?? rec.loginId ?? "사용자");
  const roleCandidate = rec.role ?? (Array.isArray(rec.roles) ? rec.roles[0] : undefined);
  return {
    username,
    displayName,
    role: normalizeRole(roleCandidate),
    staffId: rec.staffId == null || rec.staffId === "" ? undefined : Number(rec.staffId),
  };
}

export async function loginAuth(payload: { username: string; password: string }): Promise<AuthLoginResult> {
  const body = {
    username: payload.username,
    loginId: payload.username,
    password: payload.password,
  };
  const raw = await fetchJson<unknown>("/auth/login", {
    method: "POST",
    body: JSON.stringify(body),
  });
  return normalizeLoginResponse(raw, payload.username);
}

export async function meAuth(): Promise<AuthUserDto> {
  const raw = await fetchJson<unknown>("/auth/me", { method: "GET" });
  return normalizeMeResponse(raw);
}

export async function refreshAuth(): Promise<StoredAuthTokenSet> {
  const current = loadStoredAuthTokens();
  if (!current?.refreshToken) throw new Error("refreshToken이 없습니다.");
  const raw = await fetchJson<unknown>("/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken: current.refreshToken }),
    tokenOverride: undefined,
  });
  if (!raw || typeof raw !== "object") throw new Error("재발급 응답 형식 오류");
  const rec = raw as Record<string, unknown>;
  const tokenRec = (rec.token && typeof rec.token === "object") ? (rec.token as Record<string, unknown>) : rec;
  const next: StoredAuthTokenSet = {
    accessToken: String(tokenRec.accessToken ?? ""),
    refreshToken: tokenRec.refreshToken ? String(tokenRec.refreshToken) : current.refreshToken,
    tokenType: "Bearer",
  };
  if (!next.accessToken) throw new Error("재발급 accessToken 누락");
  saveStoredAuthTokens(next);
  return next;
}

export async function logoutAuth(): Promise<void> {
  const current = loadStoredAuthTokens();
  try {
    if (current?.refreshToken) {
      await fetchJson<unknown>("/auth/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken: current.refreshToken }),
      });
    }
  } finally {
    clearStoredAuthTokens();
  }
}

export async function apiFetchWithAuth(input: string, init: RequestInit = {}): Promise<Response> {
  const tokens = loadStoredAuthTokens();
  const headers = new Headers(init.headers ?? {});
  if (tokens?.accessToken && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${tokens.accessToken}`);
  }
  const first = await fetch(input, { ...init, headers });
  if (first.status !== 401) return first;
  if (!tokens?.refreshToken) return first;
  try {
    const refreshed = await refreshAuth();
    const retryHeaders = new Headers(init.headers ?? {});
    retryHeaders.set("Authorization", `Bearer ${refreshed.accessToken}`);
    return await fetch(input, { ...init, headers: retryHeaders });
  } catch {
    clearStoredAuthTokens();
    return first;
  }
}
