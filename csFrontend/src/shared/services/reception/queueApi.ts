import { API_BASES } from "@/shared/config/api";
import { fetchJsonWithAuth } from "@/shared/services/_core/http";

const ADMIN_BASE = API_BASES.admin;

export type QueueSummaryServer = {
  category: string;
  waitingCount: number;
  sampleSize: number;
  avgServiceMinutes: number;
  estimatedMinutesForNew: number;
  calculatedAt?: string;
};

export async function fetchQueueSummaryServer(category: string): Promise<QueueSummaryServer> {
  const raw = await fetchJsonWithAuth<any>(
    `${ADMIN_BASE}/admin/queue/summary?category=${encodeURIComponent(category)}`,
    { method: "GET" },
  );
  // [FIXED] ApiResponseAdvice 래핑 언래핑
  const payload = raw?.data ?? raw;

  return {
    category: String(payload?.category ?? category),
    waitingCount: Number(payload?.waitingCount ?? 0),
    sampleSize: Number(payload?.sampleSize ?? 0),
    avgServiceMinutes: Number(payload?.avgServiceMinutes ?? 0),
    estimatedMinutesForNew: Number(payload?.estimatedMinutesForNew ?? 0),
    calculatedAt: payload?.calculatedAt ? String(payload.calculatedAt) : undefined,
  };
}
