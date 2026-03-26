export const MAX_TOTAL_CAPACITY = 30;
export const MAX_EMERGENCY_COUNT = 10;

/**
 * UI 상태 + 현재 백엔드에서 내려올 수 있는 상태값을 함께 표시할 수 있도록 확장
 * (프론트 UI 표준: WAITING / IN_TREATMENT / COMPLETED)
 * (백엔드 현행: WAITING / CALLED / CLOSED 등)
 */
export const STATUS_LABEL: Record<string, string> = {
  WAITING: "대기",
  READY: "대기",
  IN_TREATMENT: "진료중",
  CALLED: "진료중",
  IN_PROGRESS: "진료중",
  COMPLETED: "완료",
  CLOSED: "완료",
  CANCELED: "취소",
  CANCELLED: "취소",
};

export const ROLE_LABEL: Record<string, string> = {
  DOC: "의사",
  ADMIN: "원무",
  SYS: "시스템관리자",
};

export const DEFAULT_UNMASK_REASON = "업무 확인";
