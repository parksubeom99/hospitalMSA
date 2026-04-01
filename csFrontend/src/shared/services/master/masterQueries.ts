// [ADDED] React Query hooks for master settings (staff list)
// 설계 원칙: enabled:false + refetch() — 기존 버튼 기반 UX 완전 보존
import { useQuery } from "@tanstack/react-query";
import { listMasterStaffServer } from "@/shared/services/master/masterStaffApi";
import type { StaffProfile } from "@/shared/types/domain";

// ── Query Keys ──────────────────────────────────────────────
export const masterQueryKeys = {
  staff: () => ["master", "staff"] as const,
};

// ── Queries ──────────────────────────────────────────────────

type UseMasterStaffQueryArgs = {
  enabled?: boolean;
};

export function useMasterStaffQuery(args: UseMasterStaffQueryArgs = {}) {
  return useQuery<StaffProfile[]>({
    queryKey: masterQueryKeys.staff(),
    queryFn: listMasterStaffServer,
    enabled: args.enabled ?? false, // [ADDED] 기본 false — 버튼 클릭 시 refetch()
    staleTime: 60_000,
    gcTime: 5 * 60_000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}
