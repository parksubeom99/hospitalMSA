import type { VisitStatus } from "@/shared/types/domain";

export type BackendVisitStatus =
  | "CREATED"
  | "READY"
  | "WAITING"
  | "CALLED"
  | "IN_PROGRESS"
  | "CLOSED"
  | "COMPLETED"
  | "CANCELED"
  | "CANCELLED";

export type FrontendRoleCode = "DOC" | "ADMIN" | "SYS";
export type BackendRoleCode = "DOC" | "ADMIN" | "SYS" | "NUR" | "LAB" | "RAD" | "PHARM" | "PROC";

export function normalizeRoleCode(input?: string | null): FrontendRoleCode | null {
  if (!input) return null;
  const x = input.trim().toUpperCase();
  if (x === "ADM") return "ADMIN"; // legacy frontend alias
  if (x === "DOC" || x === "ADMIN" || x === "SYS") return x;
  return null;
}

export function fromBackendVisitStatus(status?: string | null): VisitStatus {
  const x = (status ?? "").trim().toUpperCase();
  switch (x) {
    case "READY":
    case "WAITING":
    case "CREATED":
      return "WAITING";
    case "CALLED":
    case "IN_PROGRESS":
    case "IN_TREATMENT":
      return "IN_TREATMENT";
    case "CLOSED":
    case "COMPLETED":
      return "COMPLETED";
    default:
      return "WAITING";
  }
}

export function toBackendVisitStatus(status: VisitStatus): BackendVisitStatus {
  switch (status) {
    case "WAITING":
      return "WAITING";
    case "IN_TREATMENT":
      return "CALLED";
    case "COMPLETED":
      return "CLOSED";
  }
}

export function isManualVisitTransitionAllowed(current: VisitStatus, next: VisitStatus): boolean {
  if (current === next) return true;
  if (current === "WAITING" && next === "IN_TREATMENT") return true;
  if (current === "IN_TREATMENT" && next === "COMPLETED") return true;
  return false;
}

export function getManualVisitNextActions(current: VisitStatus): VisitStatus[] {
  if (current === "WAITING") return ["IN_TREATMENT"];
  if (current === "IN_TREATMENT") return ["COMPLETED"];
  return [];
}
