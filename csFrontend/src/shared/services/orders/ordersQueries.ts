// [ADDED] React Query hooks for orders (final orders)
// 설계 원칙: enabled:false + refetch() — 기존 버튼 기반 UX 완전 보존
import { useQuery } from "@tanstack/react-query";
import { getFinalOrdersByVisitServer } from "@/shared/services/clinicalApi";

// ── Query Keys ──────────────────────────────────────────────
export const ordersQueryKeys = {
  finalOrders: (visitId: number) => ["orders", "finalOrders", visitId] as const,
};

// ── Queries ──────────────────────────────────────────────────

type UseFinalOrdersQueryArgs = {
  visitId: number;
  enabled?: boolean;
};

export function useFinalOrdersQuery(args: UseFinalOrdersQueryArgs) {
  return useQuery<any[]>({
    queryKey: ordersQueryKeys.finalOrders(args.visitId),
    queryFn: () =>
      getFinalOrdersByVisitServer({
        visitId: args.visitId,
      }),
    enabled: args.enabled ?? false, // [ADDED] 기본 false — 버튼 클릭 시 refetch()
    staleTime: 30_000,
    gcTime: 5 * 60_000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}
