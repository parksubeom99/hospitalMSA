"use client";

import { useHospital } from "@/shared/store/HospitalStore";
import type { RoleCode } from "@/shared/types/domain";

interface RoleGateProps {
  allowed: RoleCode[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}
export function RoleGate({ allowed, children, fallback }: RoleGateProps) {
  const { state } = useHospital();
  const role = state.session?.role;
  if (!role || !allowed.includes(role)) {
    return (
      <>
        {fallback ?? (
          <div className="glass-card empty-state">
            <p>접근 권한이 없습니다.</p>
            <p className="muted">
              허용 권한: {allowed.join(", ")} / 현재 권한: {role ?? "로그인 안됨"}
            </p>
          </div>
        )}
      </>
    );
  }
  return <>{children}</>;
}
