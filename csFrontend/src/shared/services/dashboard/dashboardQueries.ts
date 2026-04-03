import { useQuery } from "@tanstack/react-query";
import { fetchDashboardSummaryServer } from "./dashboardApi";

export const dashboardSummaryQueryKey = (date: string) => ["dashboard", "summary", date] as const;

type UseDashboardSummaryQueryArgs = {
  date?: string;
  enabled?: boolean;
  refetchInterval?: number | false;
};

// [ADDED] KST 기준 오늘 날짜 반환
// 이유: new Date().toISOString()은 UTC 기준 → 한국(UTC+9) 자정~오전9시 사이
//       날짜가 하루 전으로 계산됨 → DB created_at(KST TRUNC(SYSDATE))과 불일치 → 0건
function getTodayKST(): string {
  const now = new Date();
  // UTC+9 오프셋 적용
  const kstOffset = 9 * 60; // 분 단위
  const kstMs = now.getTime() + kstOffset * 60 * 1000;
  return new Date(kstMs).toISOString().slice(0, 10);
}

export function useDashboardSummaryQuery(args: UseDashboardSummaryQueryArgs = {}) {
  // [MODIFIED] new Date().toISOString().slice(0,10) → getTodayKST()
  const date = args.date ?? getTodayKST();

  return useQuery({
    queryKey: dashboardSummaryQueryKey(date),
    queryFn: () => fetchDashboardSummaryServer({ date }),
    enabled: args.enabled ?? true,
    staleTime: 30_000,
    gcTime: 5 * 60_000,
    refetchInterval: args.refetchInterval ?? false,
    refetchOnWindowFocus: false,
  });
}
