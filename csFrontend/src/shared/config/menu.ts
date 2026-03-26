import type { RoleCode } from "@/shared/types/domain";

export interface AppMenuItem {
  key: string;
  label: string;
  href: string;
  allowedRoles: RoleCode[];
  description: string;
}

export const APP_MENUS: AppMenuItem[] = [
  { key: "reception", label: "접수", href: "/reception", allowedRoles: ["ADMIN", "SYS"], description: "예약/대기/응급 접수" },
  { key: "clinical", label: "진료", href: "/clinical", allowedRoles: ["DOC", "SYS"], description: "SOAP + 검사/영상 오더" },
  { key: "orders", label: "오더", href: "/orders", allowedRoles: ["DOC", "SYS"], description: "최종오더 (약/수술/입원/NONE)" },
  { key: "billing", label: "수납", href: "/billing", allowedRoles: ["ADMIN", "SYS"], description: "영수증 자동 생성/결제" },
  { key: "master", label: "마스터 설정", href: "/master-settings", allowedRoles: ["ADMIN", "SYS"], description: "의사/원무 프로필 관리" },
];
