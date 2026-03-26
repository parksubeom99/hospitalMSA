export type RoleCode = "DOC" | "ADMIN" | "SYS";
export type VisitStatus = "WAITING" | "IN_TREATMENT" | "COMPLETED";
export type CapacityLevel = "SAFE" | "WARN" | "DANGER" | "FULL";

export interface UserSession {
  role: RoleCode;
  displayName: string;
  username?: string;
  accessToken?: string;
  tokenType?: "Bearer";
  authSource?: "server" | "demo";
  doctorStaffId?: number;
}

export interface Patient {
  id: number;
  name: string;
  gender: "M" | "F";
  rrnFront: string;
  rrnBack: string; // mock only; real app should encrypt and fetch on demand
  phone: string;
}

export interface Reservation {
  id: number;
  patientId: number;
  reservedAt: string; // ISO
  status: "RESERVED" | "CHECKED_IN" | "CANCELLED";
  memo?: string;
  contactName?: string;
  contactPhone?: string;
}

export interface Visit {
  id: number;
  patientId: number;
  status: VisitStatus;
  registeredAt: string; // ISO
  queueNo: string;
  visitType: "WALK_IN" | "RESERVATION";
  cancelled?: boolean;
  sourceReservationId?: number;
}

export interface SoapNote {
  visitId: number;
  subjective: string;
  objective: string;
  assessment: string;
  plan: string;
  updatedAt: string;
}

export type ExamCategory = "LAB" | "RAD" | "PROC";
export interface ExamOrderItem {
  category: ExamCategory;
  code: string;
  name: string;
}

export interface MedicationCatalogItem {
  drugCode: string;
  drugName: string;
  drugGroup: string;
  unitPrice: number;
}

export interface FinalOrderMedicationItem {
  drugCode: string;
  drugName: string;
  drugGroup: string;
  qty: number;
}

export interface FinalOrderSurgery {
  surgeryType: "INTERNAL" | "EXTERNAL";
  roomNo: number;
}

export interface FinalOrderAdmission {
  wardNo: number;
  admitDate: string; // YYYY-MM-DD
  dischargeDate: string; // YYYY-MM-DD
  nights: number;
}

export interface FinalOrderInjectionItem {
  injectionCode: string;
  injectionName: string;
}

export interface FinalOrderDraft {
  visitId: number;
  types: Array<"MED" | "SURGERY" | "ADMISSION" | "NONE" | "INJECTION">;
  medications: FinalOrderMedicationItem[];
  injections?: FinalOrderInjectionItem[];
  surgery?: FinalOrderSurgery;
  admission?: FinalOrderAdmission;
  updatedAt: string;
}

export interface InvoiceItem {
  itemType: "MED" | "SURGERY" | "ADMISSION" | "INJECTION";
  itemName: string;
  qty: number;
  unitPrice: number;
  amount: number;
  metaLabel?: string;
}

export interface Invoice {
  invoiceId: number;
  visitId: number;
  status: "UNPAID" | "PAID";
  createdAt: string;
  items: InvoiceItem[];
  totalAmount: number;
  paidAt?: string;
  paymentMethod?: "CARD" | "CASH";
}

export interface StaffProfile {
  staffId: number;
  staffName: string;
  jobType: "DOCTOR" | "ADMIN";
  department: string;
  specialty?: string;
  phone: string;
  email: string;
  active: boolean;
}

export interface CapacitySummary {
  max: number;
  current: number;
  level: CapacityLevel;
  canRegister: boolean;
  waitingAndInTreatment: number;
  reservation: number;
  emergency: number;
}

export interface HospitalState {
  session: UserSession | null;
  emergencyCount: number;
  patients: Patient[];
  reservations: Reservation[];
  visits: Visit[];
  soaps: Record<number, SoapNote>;
  examOrders: Record<number, ExamOrderItem[]>;
  finalOrders: Record<number, FinalOrderDraft>;
  invoices: Invoice[];
  staff: StaffProfile[];
  rrnUnmaskAudit: Array<{
    visitId: number;
    patientId: number;
    roleCode: RoleCode;
    reason: string;
    createdAt: string;
  }>;
}
