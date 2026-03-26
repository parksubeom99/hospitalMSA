import { useQuery } from "@tanstack/react-query";
import { fetchQueueSummaryServer } from "./queueApi";

export const queueSummaryQueryKey = (category: string) => ["queue", "summary", category] as const;

type UseQueueSummaryQueryArgs = {
  category: string;
  enabled?: boolean;
  refetchInterval?: number | false;
};

export function useQueueSummaryQuery(args: UseQueueSummaryQueryArgs) {
  return useQuery({
    queryKey: queueSummaryQueryKey(args.category),
    queryFn: () => fetchQueueSummaryServer(args.category),
    enabled: args.enabled ?? true,
    staleTime: 15_000,
    gcTime: 5 * 60_000,
    refetchInterval: args.refetchInterval ?? false,
    refetchOnWindowFocus: false,
  });
}
