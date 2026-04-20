// [ADDED] "use client" — LoginScreen은 클라이언트 컴포넌트 (useHospital hook 사용)
"use client";

import { LoginScreen } from "@/features/auth/LoginScreen";

export default function LoginPage() {
  return <LoginScreen />;
}
