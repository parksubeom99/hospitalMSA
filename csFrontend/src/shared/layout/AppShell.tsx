"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { APP_MENUS } from "@/shared/config/menu";
import { ROLE_LABEL } from "@/shared/config/constants";
import { useHospital } from "@/shared/store/HospitalStore";

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { state, logout, resetDemoData } = useHospital();
  const role = state.session?.role;

  return (
    <div className="app-bg">
      <div className="app-bg__image" />
      <div className="app-bg__veil" />
      <div className="app-container">
        <header className="top-header">
          <div className="brand">
            <Link href="/">Samson Hospital</Link>
          </div>

          <nav className="top-nav" aria-label="주요 메뉴">
            {APP_MENUS.map((menu) => {
              const allowed = role ? menu.allowedRoles.includes(role) : false;
              const active = pathname === menu.href;
              return (
                <Link
                  key={menu.key}
                  href={menu.href}
                  className={`top-nav__link ${active ? "is-active" : ""} ${allowed ? "" : "is-locked"}`}
                  title={`${menu.label} / 허용 권한: ${menu.allowedRoles.join(", ")}`}
                >
                  {menu.label}
                </Link>
              );
            })}
          </nav>

          <div className="header-user">
            <span className={`header-user__role ${role ? "" : "is-empty"}`}>
              {role ? `${state.session?.displayName ?? ROLE_LABEL[role]}(${role})` : "로그아웃"}
            </span>
            <Link href="/login" className="header-user__btn">로그인</Link>
            <button type="button" className="header-user__ghost" onClick={resetDemoData}>데모초기화</button>
            {role ? (
              <button type="button" className="header-user__ghost" onClick={logout}>로그아웃</button>
            ) : null}
          </div>
        </header>

        <main className="page-wrap">{children}</main>
      </div>
    </div>
  );
}
