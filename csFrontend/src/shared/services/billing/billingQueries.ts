// [ADDED] React Query hooks for billing (invoices)
// 설계 원칙: enabled:false + refetch() — 기존 버튼 기반 UX 완전 보존
import { useQuery } from "@tanstack/react-query";
import { listInvoicesServer, type BackendInvoice } from "@/shared/services/billing/billingApi";

// ── Query Keys ──────────────────────────────────────────────
export const billingQueryKeys = {
  invoices: (visitId: number) => ["billing", "invoices", visitId] as const,
};

// ── Queries ──────────────────────────────────────────────────

type UseInvoicesQueryArgs = {
  visitId: number;
  enabled?: boolean;
};

export function useInvoicesQuery(args: UseInvoicesQueryArgs) {
  return useQuery<BackendInvoice[]>({
    queryKey: billingQueryKeys.invoices(args.visitId),
    queryFn: () => listInvoicesServer({ visitId: args.visitId }),
    enabled: args.enabled ?? false, // [ADDED] 기본 false — 버튼 클릭 시 refetch()
    staleTime: 30_000,
    gcTime: 5 * 60_000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}
