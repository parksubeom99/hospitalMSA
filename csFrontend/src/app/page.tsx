// [ADDED] "use client" — DashboardScreen은 클라이언트 컴포넌트
// SSR 경계 불명확 시 Next.js 14 App Router에서 hydration 불일치 발생
"use client";

import { DashboardScreen } from "@/features/dashboard/DashboardScreen";

export default function HomePage() {
  return <DashboardScreen />;
}
