import { API_BASES } from "@/shared/config/api";
import { fetchJsonWithAuth } from "@/shared/services/_core/http";
import { toBackendVisitStatus } from "@/shared/lib/integrationBridge";
import type { VisitStatus } from "@/shared/types/domain";

type HeadersInput = { accessToken?: string; tokenType?: string };
const ADMIN_BASE = API_BASES.admin;

export async function upsertPatientForReception(args: {
  session?: HeadersInput;
  patientId: number;
  name: string;
  gender: "M" | "F";
  rrnFront: string;
  rrnBack: string;
  phone: string;
}) {
  const rrnMasked = `${args.rrnFront}-${(args.rrnBack || "").slice(0,1)}******`;
  return fetchJsonWithAuth<any>(`${ADMIN_BASE}/master/patients`, {
    method: "POST",
    body: JSON.stringify({
      patientId: args.patientId,
      name: args.name.trim(),
      gender: args.gender,
      rrnMasked,
      phone: args.phone.trim(),
      active: true,
    }),
  });
}

export async function createReservationServer(args: {
  session?: HeadersInput;
  patientId: number;
  patientName: string;
  reservedAtIso: string;
}) {
  return fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/reservations`, {
    method: "POST",
    body: JSON.stringify({
      patientId: args.patientId,
      patientName: args.patientName,
      departmentCode: "IM",
      doctorId: null,
      scheduledAt: args.reservedAtIso,
    }),
  });
}

export async function checkInReservationServer(args: { session?: HeadersInput; reservationId: number }) {
  return fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/reservations/${args.reservationId}/check-in`, { method: "POST" });
}

export async function cancelVisitServer(args: { session?: HeadersInput; visitId: number; reason?: string }) {
  const q = args.reason ? `?reason=${encodeURIComponent(args.reason)}` : "";
  return fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/visits/${args.visitId}/cancel${q}`, { method: "POST" });
}

export async function createVisitServer(args: {
  session?: HeadersInput;
  patientId: number;
  patientName: string;
  mode: "WALK_IN" | "RESERVATION";
}) {
  return fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/visits`, {
    method: "POST",
    body: JSON.stringify({
      patientId: args.patientId,
      patientName: args.patientName,
      departmentCode: "IM",
      doctorId: null,
      arrivalType: args.mode === "WALK_IN" ? "WALK_IN" : "RESERVATION",
      triageLevel: null,
    }),
  });
}

export async function updateVisitServer(args: {
  session?: HeadersInput;
  visitId: number;
  patientName: string;
}) {
  return fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/visits/${args.visitId}`, {
    method: "PUT",
    body: JSON.stringify({
      patientName: args.patientName,
      departmentCode: "IM",
      doctorId: null,
      arrivalType: null,
      triageLevel: null,
    }),
  });
}

export async function updateVisitStatusServer(args: {
  session?: HeadersInput;
  visitId: number;
  status: VisitStatus;
}) {
  return fetchJsonWithAuth<any>(`${ADMIN_BASE}/admin/visits/${args.visitId}/status`, {
    method: "POST",
    body: JSON.stringify({ status: toBackendVisitStatus(args.status) }),
  });
}
