function trimSlash(value: string): string {
  return value.replace(/\/$/, "");
}

// [MODIFIED] process.env[key] → 리터럴 접근으로 변경
// Next.js는 리터럴 접근만 빌드 타임 번들에 인라인함
const gatewayBase = process.env.NEXT_PUBLIC_GATEWAY_URL
  ? trimSlash(process.env.NEXT_PUBLIC_GATEWAY_URL.trim())
  : undefined;

function resolveServiceBase(fallback: string): string {
  if (gatewayBase) return `${gatewayBase}/api`;
  return fallback;
}

export const API_BASES = {
  iam:      resolveServiceBase("http://localhost:8181"),
  admin:    resolveServiceBase("http://localhost:8182"),
  clinical: resolveServiceBase("http://localhost:8183"),
  support:  resolveServiceBase("http://localhost:8184"),
} as const;

export const APP_TRANSPORT = {
  gatewayBase,
  usesGateway: Boolean(gatewayBase),
} as const;