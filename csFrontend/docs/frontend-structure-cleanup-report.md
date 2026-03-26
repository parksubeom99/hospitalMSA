# Frontend Structure Cleanup Report (csFrontend)

## 목적
프론트(Next.js) 프로젝트 루트에서 혼합/생성 파일을 정리하고, `shared/services`를 한눈에 구분 가능한 구조로 재분류했습니다.

## 주요 변경 사항

### 1) 루트 정리 (프론트 기준)
- 제거(생성물): `.gradle/`, `bin/`, `tsconfig.tsbuildinfo`
- 분리(보관): 루트의 `build.gradle` → `docs/_archived_mixed_backend/build.gradle`
  - 이유: Next.js 프론트 실행에는 필요 없고, Spring Boot 백엔드 흔적 파일이었음

### 2) 서비스 파일 재분류 (`src/shared/services`)
기존 평면 구조를 기능별 하위 폴더로 정리:

- `src/shared/services/_core/`
  - `contracts.ts`
  - `tokenStorage.ts`
- `src/shared/services/auth/`
  - `authApi.ts`
- `src/shared/services/billing/`
  - `billingApi.ts`
- `src/shared/services/clinical/`
  - `clinicalApi.ts`
- `src/shared/services/dashboard/`
  - `dashboardApi.ts`
- `src/shared/services/master/`
  - `masterStaffApi.ts`
- `src/shared/services/reception/`
  - `receptionApi.ts`
  - `receptionMutationApi.ts`

### 3) 호환성 유지 (중요)
기존 import 경로가 깨지지 않도록 `src/shared/services/*.ts`에 **re-export shim** 파일을 남겨 두었습니다.

예:
- 기존 `@/shared/services/authApi` import 계속 사용 가능
- 내부 구현은 `@/shared/services/auth/authApi`로 정리됨

### 4) 불필요 중복 파일 정리
- 삭제: `src/features/clinical/clinicalApi.ts`
  - 사유: `src/shared/services/clinicalApi.ts`와 **완전 동일 내용**이며 실제 사용 import도 shared/services 쪽만 사용

## 권장 다음 단계 (선택)
- 점진적으로 import를 shim 경로 대신 새 경로로 교체
  - 예: `@/shared/services/clinical/clinicalApi`
- `shared/components`도 용도별(`charts`, `guards`, `display`) 세분화 가능

## 실행 참고 (프론트)
```bash
npm install
npm run dev
```
