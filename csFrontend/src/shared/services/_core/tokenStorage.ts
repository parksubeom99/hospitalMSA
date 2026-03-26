export const AUTH_STORAGE_KEY = "hospital-msa-auth-v1";

export interface StoredAuthTokenSet {
  accessToken: string;
  refreshToken?: string;
  tokenType?: "Bearer";
}

export function loadStoredAuthTokens(): StoredAuthTokenSet | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<StoredAuthTokenSet>;
    if (!parsed?.accessToken || typeof parsed.accessToken !== "string") return null;
    return {
      accessToken: parsed.accessToken,
      refreshToken: typeof parsed.refreshToken === "string" ? parsed.refreshToken : undefined,
      tokenType: parsed.tokenType === "Bearer" ? "Bearer" : "Bearer",
    };
  } catch {
    return null;
  }
}

export function saveStoredAuthTokens(tokens: StoredAuthTokenSet): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(tokens));
}

export function clearStoredAuthTokens(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
}
