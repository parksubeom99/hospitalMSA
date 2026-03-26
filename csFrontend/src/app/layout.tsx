import "@/styles/globals.css";
import type { Metadata } from "next";
import { Providers } from "./providers";
import { AppShell } from "@/shared/layout/AppShell";

export const metadata: Metadata = {
  title: "Hospital MSA 4서비스 프론트 (AdminCore 통합)",
  description: "접수/진료/오더/수납/마스터 설정 데모 프론트",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          <AppShell>{children}</AppShell>
        </Providers>
      </body>
    </html>
  );
}
