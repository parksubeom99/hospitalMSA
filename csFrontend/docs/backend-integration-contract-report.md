# 백엔드 연동 준비 계약 고정 보고서 (프론트 기준)

## 이번 결정(고정)
- 서비스 토폴로지: **4서비스**
  - `iamService`
  - `adminMasterService` (Admin + Master 통합)
  - `clinicalService`
  - `supportService`
- 프론트 권한 코드: **DOC / ADMIN / SYS**
  - 과거 `ADM`은 레거시 alias로만 허용(정규화 시 `ADMIN`으로 변환)
- 응급 정책: **프론트 A안 유지(0~10 카운터)**
  - 현재 백엔드는 전용 카운터 API가 없어 `dashboard/summary` 파생으로 읽기만 가능
- 접수 상태 UI 표준: **WAITING -> IN_TREATMENT -> COMPLETED**
  - 백엔드 현행: **WAITING -> CALLED -> CLOSED**
  - 프론트에서 상태 매핑 브리지 사용

## 마스터 코드를 어디에 두는 게 맞는가?
현 상태(5 -> 4 서비스 전환)에서는 **adminMasterService에 두는 것이 맞습니다.**

### 이유
- IAM은 인증/인가/JWT/계정 보안 책임에 집중하는 것이 분리 원칙에 맞음
- 마스터(직원 프로필/부서/코드)는 운영/업무 관리 성격이 강해 Admin과 결합도가 높음
- 현재 업로드된 백엔드 구현도 `/master/staff`가 `adminMasterService` 안에 존재함

## 설계안 vs 현재 백엔드(직접 연동) 차이 처리 전략
1. **프론트 UI 계약(UiContracts)은 설계안 유지**
2. **BackendContracts에 현재 백엔드 경로/메서드 기록**
3. 실제 연동 시 adapter에서 변환
   - 경로 변환
   - 메서드 변환(PATCH -> POST 등)
   - 상태값 변환(IN_TREATMENT <-> CALLED)

## 이번 프론트 수정 반영 요약
- `ADM` -> `ADMIN` 전면 정리 (로그인/권한가드/메뉴/문구)
- `.env.example`를 4서비스 기준으로 정리 (`ADMIN_MASTER/CLINICAL/SUPPORT` 중심)
- 레거시 env alias 유지 (마이그레이션 중 혼란 방지)
- 상태 라벨 확장 (`CALLED`, `CLOSED`, `READY`, `IN_PROGRESS`도 표시 가능)
- 접수 수동 상태변경을 **순차 전이만 허용**하도록 수정
- 레거시 localStorage 세션 role(`ADM`) 자동 정규화 처리
- 계약 고정 파일(`src/shared/services/contracts.ts`)을 UI 계약 / 백엔드 계약 병행 관리 형태로 개편

## 백엔드 연동 시작 전에 우선 구현/정리 필요 (백엔드 쪽 TODO)
- `GET/PATCH /admin/emergency-counter` (A안 전용)
- `GET /admin/reception/visits/{visitId}/rrn?unmask=true` (+ privacy audit)
- `POST /admin/billing/invoices/auto-from-final-order` 또는 동등한 adapter/BFF
- 권한 문자열 통일: 최소한 프론트 사용 범위 API는 `ADMIN` 허용 포함 상태 점검

