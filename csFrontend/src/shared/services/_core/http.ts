import { apiFetchWithAuth } from "@/shared/services/authApi";

function safeJsonParse(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function extractMessage(data: unknown, status: number, fallback?: string): string {
  if (data && typeof data === "object") {
    const rec = data as Record<string, unknown>;
    if (typeof rec.message === "string" && rec.message.trim()) return rec.message;
    if (typeof rec.error === "string" && rec.error.trim()) return rec.error;
  }
  if (typeof data === "string" && data.trim()) return data;
  return fallback || `HTTP ${status}`;
}

export async function fetchJsonWithAuth<T>(url: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers ?? {});
  if (!headers.has("Content-Type") && init.method && init.method !== "GET") {
    headers.set("Content-Type", "application/json");
  }
  const res = await apiFetchWithAuth(url, { ...init, headers, cache: "no-store" });
  if (res.status === 204) return undefined as T;
  const text = await res.text().catch(() => "");
  const data = text ? safeJsonParse(text) : undefined;
  if (!res.ok) {
    throw new Error(extractMessage(data, res.status));
  }
  return data as T;
}
