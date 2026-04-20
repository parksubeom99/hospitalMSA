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
// [MODIFIED v3.3] reservations 키: date 생략 가능 (전체 예약 조회 모드 지원)
export const receptionQueryKeys = {
  reservations: (date?: string) => ["reception", "reservations", date ?? "__all__"] as const,
  visits: (statuses?: string[]) => ["reception", "visits", statuses ?? []] as const,
};

// ── Queries ──────────────────────────────────────────────────

type UseReservationsQueryArgs = {
  date?: string;
  enabled?: boolean;
};

export function useReservationsQuery(args: UseReservationsQueryArgs = {}) {
  // [MODIFIED v3.3] date 기본값 제거 — 전달 시에만 필터, 없으면 전체 예약 조회
  // 이전 버그: 항상 오늘 기준 → 미래 일자(4/23 발표일) 예약이 목록에 안 뜸
  const date = args.date;
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
