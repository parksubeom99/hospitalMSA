// [MODIFIED] React Query hooks for reception (reservations + visits)
// 설계 원칙: enabled:true — 화면 진입 시 자동 서버 조회, staleTime으로 불필요한 재요청 방지
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchReservationsServer,
  fetchVisitsServer,
  type SyncedReservationRow,
  type SyncedVisitRow,
} from "./receptionApi";
import {
  cancelVisitServer,
  checkInReservationServer,
  createReservationServer,
  createVisitServer,
  updateVisitServer,
  upsertPatientForReception,
} from "./receptionMutationApi";
import { getTodayKST } from "@/shared/lib/date"; // [ADDED] UTC/KST 버그 #3 차단

// ── Query Keys ──────────────────────────────────────────────
export const receptionQueryKeys = {
  reservations: (date: string) => ["reception", "reservations", date] as const,
  visits: (statuses?: string[]) => ["reception", "visits", statuses ?? []] as const,
};

// ── Queries ──────────────────────────────────────────────────

type UseReservationsQueryArgs = {
  date?: string;
  enabled?: boolean;
};

export function useReservationsQuery(args: UseReservationsQueryArgs = {}) {
  // [MODIFIED] new Date().toISOString().slice(0,10) (UTC) → getTodayKST() (KST)
  // 이유: 한국 자정~오전 9시에 UTC 기준 날짜는 전날 → 오늘 예약 0건 (버그 #3)
  const date = args.date ?? getTodayKST();
  return useQuery<SyncedReservationRow[]>({
    queryKey: receptionQueryKeys.reservations(date),
    queryFn: () => fetchReservationsServer({ date }),
    enabled: args.enabled ?? true,
    staleTime: 30_000,
    gcTime: 5 * 60_000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}

type UseVisitsQueryArgs = {
  statuses?: string[];
  enabled?: boolean;
};

export function useVisitsQuery(args: UseVisitsQueryArgs = {}) {
  return useQuery<SyncedVisitRow[]>({
    queryKey: receptionQueryKeys.visits(args.statuses),
    queryFn: () => fetchVisitsServer({ statuses: args.statuses }),
    enabled: args.enabled ?? true,
    staleTime: 30_000,
    gcTime: 5 * 60_000,
    refetchOnWindowFocus: false,
    retry: 1,
  });
}

// ── Mutations ────────────────────────────────────────────────

export function useUpsertPatientMutation() {
  return useMutation({
    mutationFn: upsertPatientForReception,
  });
}

export function useCreateReservationMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createReservationServer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["reception", "reservations"] });
    },
  });
}

export function useCheckInReservationMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: checkInReservationServer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["reception", "reservations"] });
      qc.invalidateQueries({ queryKey: ["reception", "visits"] });
    },
  });
}

export function useCreateVisitMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createVisitServer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["reception", "visits"] });
    },
  });
}

export function useUpdateVisitMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: updateVisitServer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["reception", "visits"] });
    },
  });
}

export function useCancelVisitMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: cancelVisitServer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["reception", "visits"] });
    },
  });
}
