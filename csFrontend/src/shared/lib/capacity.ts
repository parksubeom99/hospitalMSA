import { MAX_TOTAL_CAPACITY } from "@/shared/config/constants";
import type { CapacityLevel, CapacitySummary } from "@/shared/types/domain";

export function getCapacityLevel(current: number, max = MAX_TOTAL_CAPACITY): CapacityLevel {
  if (current >= max) return "FULL";
  const ratio = current / max;
  if (ratio >= 0.85) return "DANGER";
  if (ratio >= 0.6) return "WARN";
  return "SAFE";
}

export function ringColorByLevel(level: CapacityLevel): string {
  switch (level) {
    case "SAFE": return "#4ade80";
    case "WARN": return "#f59e0b";
    case "DANGER": return "#f97316";
    case "FULL": return "#ef4444";
  }
}

export function glowClassByLevel(level: CapacityLevel): string {
  switch (level) {
    case "SAFE": return "glow-safe";
    case "WARN": return "glow-warn";
    case "DANGER":
    case "FULL":
      return "glow-danger";
  }
}

export function canRegister(summary: Pick<CapacitySummary, "current" | "max">) {
  return summary.current < summary.max;
}
