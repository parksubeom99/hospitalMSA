import { useQuery } from "@tanstack/react-query";
import { fetchDashboardSummaryServer } from "./dashboardApi";

export const dashboardSummaryQueryKey = (date: string) => ["dashboard", "summary", date] as const;

type UseDashboardSummaryQueryArgs = {
  date?: string;
  enabled?: boolean;
  refetchInterval?: number | false;
};

export function useDashboardSummaryQuery(args: UseDashboardSummaryQueryArgs = {}) {
  const date = args.date ?? new Date().toISOString().slice(0, 10);

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
