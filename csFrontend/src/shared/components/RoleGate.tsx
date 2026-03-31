"use client";

import { useHospital } from "@/shared/store/HospitalStore";
import type { RoleCode } from "@/shared/types/domain";

interface RoleGateProps {
  allowed: RoleCode[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export function RoleGate({ allowed, children, fallback }: RoleGateProps) {
  const { hydrated, state } = useHospital(); // [MODIFIED] hydrated 추가

  // [ADDED] hydration guard — React #418/#423/#425 해소
  // 원인: SSR session=null → "접근 권한 없음" DOM
  //       Client localStorage 복원 후 → session 존재 → 전체 폼 DOM
  //       두 트리 불일치 → hydration mismatch 폭발
  // 해결: localStorage 복원(useEffect) 완료 전 null 반환
  //       → SSR DOM = 최초 client DOM = null → 일치 → mismatch 없음
  //       → hydrated=true 이후 실제 session 기반 정상 렌더
  if (!hydrated) return null;

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

// [REMOVED] export * from "./master/masterStaffApi"
// 이유: UI 컴포넌트 파일에서 API re-export → 책임 혼재 + 번들 오염 위험 제거
//       masterStaffApi는 @/shared/services/masterStaffApi 직접 import 사용
