// [ADDED] React Query hooks for clinical (SOAP + exam orders)
// 설계 원칙: enabled:false + refetch() — 기존 버튼 기반 UX 완전 보존
import { useQuery } from "@tanstack/react-query";
import {
  getExamOrdersByVisitServer,
  getSoapServer,
} from "@/shared/services/clinical/clinicalApi";
import type { ExamOrderItem } from "@/shared/types/domain";

type SoapData = {
  visitId: number;
  subjective: string;
  objective: string;
  assessment: string;
  plan: string;
  updatedAt: string;
};

// ── Query Keys ──────────────────────────────────────────────
export const clinicalQueryKeys = {
  soap: (visitId: number) => ["clinical", "soap", visitId] as const,
  examOrders: (visitId: number) => ["clinical", "examOrders", visitId] as const,
};

// ── Queries ──────────────────────────────────────────────────

type UseSoapQueryArgs = {
  visitId: number;
  enabled?: boolean;
};

export function useSoapQuery(args: UseSoapQueryArgs) {
  return useQuery<SoapData>({
    queryKey: clinicalQueryKeys.soap(args.visitId),
    queryFn: () => getSoapServer({ visitId: args.visitId }),
    enabled: args.enabled ?? false, // [ADDED] 기본 false — 버튼 클릭 시 refetch()
    staleTime: 30_000,
    gcTime: 5 * 60_000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}

type UseExamOrdersQueryArgs = {
  visitId: number;
  enabled?: boolean;
};

export function useExamOrdersQuery(args: UseExamOrdersQueryArgs) {
  return useQuery<ExamOrderItem[]>({
    queryKey: clinicalQueryKeys.examOrders(args.visitId),
    queryFn: () => getExamOrdersByVisitServer({ visitId: args.visitId }),
    enabled: args.enabled ?? false, // [ADDED] 기본 false — 버튼 클릭 시 refetch()
    staleTime: 30_000,
    gcTime: 5 * 60_000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}
