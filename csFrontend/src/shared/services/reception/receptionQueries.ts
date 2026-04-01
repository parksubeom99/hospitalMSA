// [ADDED] React Query hooks for reception (reservations + visits)
// 설계 원칙: enabled:false + refetch() — 기존 버튼 기반 UX 완전 보존
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
  const date = args.date ?? new Date().toISOString().slice(0, 10);
  return useQuery<SyncedReservationRow[]>({
    queryKey: receptionQueryKeys.reservations(date),
    queryFn: () => fetchReservationsServer({ date }),
    enabled: args.enabled ?? false, // [ADDED] 기본 false — 버튼 클릭 시 refetch()
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
    enabled: args.enabled ?? false, // [ADDED] 기본 false — 버튼 클릭 시 refetch()
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
