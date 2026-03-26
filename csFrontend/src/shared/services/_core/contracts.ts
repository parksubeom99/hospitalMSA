/**
 * 프론트(설계안 기준) 계약과 현재 백엔드(업로드된 4서비스 코드) 계약을 함께 관리합니다.
 * 목적: mock -> real 전환 시 경로/메서드/상태값 불일치를 한 곳에서 정리하기 위함.
 */

export const ContractDecision = {
  serviceTopology: "4-services",
  masterPlacement: "adminMasterService", // ✅ 현재 백엔드 구현 기준
  frontendRoleCodes: ["DOC", "ADMIN", "SYS"] as const,
  emergencyPolicyFrontend: "A", // 0~10 카운터 UI 유지
  emergencyPolicyBackendCurrent: "B-ish (dashboard summary derived)", // 업로드본 기준
  visitStatusFrontend: ["WAITING", "IN_TREATMENT", "COMPLETED"] as const,
  visitStatusBackendCurrent: ["WAITING", "CALLED", "CLOSED"] as const,
} as const;

/** 설계안 기준(프론트 화면/기획 기준) */
export const UiContracts = {
  dashboardSummary: { method: "GET", path: "/admin/dashboard/summary" },
  dashboardReceptionStatus: { method: "GET", path: "/admin/dashboard/reception-status" },
  dashboardReservationStatus: { method: "GET", path: "/admin/dashboard/reservation-status" },
  emergencyCounterGet: { method: "GET", path: "/admin/emergency-counter" },
  emergencyCounterPatch: { method: "PATCH", path: "/admin/emergency-counter" },
  receptionVisitsList: { method: "GET", path: "/admin/reception/visits" },
  receptionVisitCreate: { method: "POST", path: "/admin/reception/visits" },
  receptionVisitRrn: (visitId: number) => ({ method: "GET" as const, path: `/admin/reception/visits/${visitId}/rrn` }),
  receptionVisitStatusPatch: (visitId: number) => ({ method: "PATCH" as const, path: `/admin/reception/visits/${visitId}/status` }),
  reservations: { method: "POST/GET", path: "/admin/reservations" },
  soapByVisit: (visitId: number) => ({ method: "GET/PUT" as const, path: `/clinical/emr/soaps/${visitId}` }),
  orders: { method: "POST", path: "/clinical/orders" },
  finalOrders: { method: "POST/GET", path: "/clinical/final-orders" },
  billingAutoFromFinalOrder: { method: "POST", path: "/admin/billing/invoices/auto-from-final-order" },
  billingPayments: { method: "POST", path: "/admin/billing/payments" },
  admissions: { method: "GET", path: "/clinical/admissions" },
  masterStaff: { method: "GET/POST/PUT/DELETE", path: "/master/staff" },
} as const;

/** 현재 업로드된 백엔드 코드 기준(직접 연동 시) */
export const BackendContracts = {
  dashboardSummary: { method: "GET", path: "/admin/dashboard/summary" },

  // 대시보드 세부 카드 분리 API는 현재 없음 -> summary에서 파생
  dashboardReceptionStatus: { method: "DERIVE_FROM_SUMMARY", path: "/admin/dashboard/summary" },
  dashboardReservationStatus: { method: "DERIVE_FROM_SUMMARY", path: "/admin/dashboard/summary" },

  // 응급 카운터 A안 전용 endpoint 미구현 -> 임시로 summary.distribution.emergency 사용
  emergencyCounterGet: { method: "DERIVE_FROM_SUMMARY", path: "/admin/dashboard/summary" },
  emergencyCounterPatch: { method: "TODO_BACKEND", path: "/admin/emergency-counter" },

  reservationsList: { method: "GET", path: "/admin/reservations" },
  reservationsCreate: { method: "POST", path: "/admin/reservations" },

  visitsList: { method: "GET", path: "/admin/visits" },
  visitsCreate: { method: "POST", path: "/admin/visits" },
  visitStatusChange: (visitId: number) => ({ method: "POST" as const, path: `/admin/visits/${visitId}/status` }), // body.status = WAITING/CALLED/CLOSED

  // 주민번호 언마스킹 전용 endpoint는 현재 없음 -> TODO_BACKEND (privacy audit 포함)
  visitRrnUnmask: (visitId: number) => ({ method: "TODO_BACKEND" as const, path: `/admin/reception/visits/${visitId}/rrn` }),

  soapByVisit: (visitId: number) => ({ method: "GET/PUT" as const, path: `/emr/soaps/${visitId}` }),
  orders: { method: "POST", path: "/orders" },
  finalOrders: { method: "POST/GET", path: "/final-orders" },

  // auto-from-final-order 미구현: 프론트/어댑터에서 Clinical 조회 후 /admin/billing/invoices 로 생성 필요
  billingAutoFromFinalOrder: { method: "ADAPTER_COMPOSE", path: "/admin/billing/invoices" },
  billingPayments: { method: "POST", path: "/admin/billing/payments" },

  // 설계안의 /clinical/admissions 대신 현재 백엔드는 adminMasterService 루트 /admissions 사용
  admissions: { method: "GET", path: "/admissions" },
  masterStaff: { method: "GET/POST/PUT/DELETE", path: "/master/staff" },
} as const;

/**
 * 프론트에서 우선 참조할 대표 계약(연동 직전 단계용):
 * - UI/설계안 path를 유지하되, 실제 연동 시에는 BackendContracts로 bridge/adaptor를 둡니다.
 */
export const ApiContracts = {
  dashboardSummary: UiContracts.dashboardSummary.path,
  dashboardReceptionStatus: UiContracts.dashboardReceptionStatus.path,
  dashboardReservationStatus: UiContracts.dashboardReservationStatus.path,
  emergencyCounter: UiContracts.emergencyCounterGet.path,
  receptionVisits: UiContracts.receptionVisitsList.path,
  receptionVisitRrn: (visitId: number) => UiContracts.receptionVisitRrn(visitId).path,
  reservations: UiContracts.reservations.path,
  soapByVisit: (visitId: number) => UiContracts.soapByVisit(visitId).path,
  orders: UiContracts.orders.path,
  finalOrders: UiContracts.finalOrders.path,
  billingAutoFromFinalOrder: UiContracts.billingAutoFromFinalOrder.path,
  billingPayments: UiContracts.billingPayments.path,
  admissions: UiContracts.admissions.path,
  masterStaff: UiContracts.masterStaff.path,
} as const;
