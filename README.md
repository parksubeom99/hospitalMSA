# 🏥 Hospital MSA — 병원 통합 관리 시스템

> Spring Boot MSA + Next.js 14 + Kafka Outbox/Saga + AWS EC2/RDS 실배포 포트폴리오

**🌐 배포 URL:** http://3.107.93.201:3000  
**계정:** system / system (SYS 권한 — 전 화면 접근 가능)

---

## 📐 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Client (Next.js 14)                   │
│            App Router · React Query v5 · Zustand         │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP (3000)
┌───────────────────────▼─────────────────────────────────┐
│            Spring Cloud Gateway (8180)                   │
│         JWT Pre-Auth Filter · CORS · Routing             │
└──┬────────────┬─────────────┬──────────────┬────────────┘
   │            │             │              │
   ▼            ▼             ▼              ▼
┌──────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐
│ IAM  │  │  Admin  │  │ Clinical │  │ Support  │
│ 8181 │  │  8182   │  │  8183    │  │  8184    │
│      │  │         │  │          │  │          │
│ JWT  │  │ Patient │  │ SOAP/EMR │  │ Lab/RAD  │
│Redis │  │ Visit   │  │ Order    │  │ Pharmacy │
│ RBAC │  │ Billing │  │ Outbox   │  │ Outbox   │
└──────┘  └────┬────┘  └────┬─────┘  └────┬─────┘
               │             │              │
               └──────┬──────┘              │
                      ▼                     ▼
              ┌───────────────┐    ┌────────────────┐
              │  Apache Kafka │    │   MySQL/RDS     │
              │  Zookeeper    │◄───│  (AWS db.t4g)  │
              │  Outbox Saga  │    └────────────────┘
              └───────────────┘
                      │
              ┌───────▼───────┐
              │  Grafana 3001 │
              │  Tempo 3200   │
              │  Distributed  │
              │  Tracing      │
              └───────────────┘
```

---

## 🛠 기술 스택 및 선택 근거

### Backend
| 기술 | 버전 | 선택 근거 |
|---|---|---|
| Spring Boot | 3.x | 국내 핀테크 표준 스택. 토스뱅크·카카오페이 공통 |
| Spring Cloud Gateway | 4.x | MSA 단일 진입점. JWT 사전 인증 필터 적용 |
| Spring Security | 6.x | RBAC 권한 체계 (SYS/ADMIN/DOC/NUR/LAB/RAD/PHARM) |
| Kafka + Outbox Pattern | 3.x | 서비스 간 이벤트 결합도 제거. 분산 트랜잭션 보장 |
| Flyway | 9.x | DB 마이그레이션 버전 관리 |
| JPA/Hibernate | 6.x | ORM. Specification 패턴으로 동적 쿼리 구현 |
| Redis | 7.x | Refresh Token 저장소 |

### Frontend
| 기술 | 버전 | 선택 근거 |
|---|---|---|
| Next.js | 14 (App Router) | SSR/CSR 하이브리드. 국내 핀테크 채용 선호 스택 |
| React Query (TanStack) | v5 | 서버 상태 관리. 캐싱·자동 갱신·낙관적 업데이트 |
| TypeScript | 5.x | 타입 안정성. 대규모 코드베이스 유지보수 |
| Zustand | 4.x | 클라이언트 상태 (HospitalStore) |

### Infrastructure
| 기술 | 선택 근거 |
|---|---|
| AWS EC2 t3.small | 실제 클라우드 배포 경험. 비용 최적화 |
| AWS RDS MySQL 8.4 | 관리형 DB. 토스뱅크·카카오 MySQL(Aurora) 동일 계열 |
| Docker Compose | 9개 컨테이너 오케스트레이션 |
| Grafana + Tempo | 분산 추적. OpenTelemetry(OTLP) 수집 |
| GitHub Actions | CI 파이프라인 (JUnit5 → 자동 빌드) |

---

## 📋 Phase별 개발 이력

| Phase | 내용 | 상태 |
|---|---|---|
| Phase 1 | MySQL full-stack Docker Compose (4서비스 + 프론트) | ✅ |
| Phase 2-A | Oracle XE 21c 포팅 (MySQL→Oracle 마이그레이션 경험) | ✅ |
| Phase 2-B | React Query (TanStack v5) 5화면 서버 상태 연동 | ✅ |
| Phase 2-C | Kafka Choreography Saga + Outbox Pattern | ✅ |
| Phase 2-D | JUnit5 단위 테스트 7개 + GitHub Actions CI | ✅ |
| Phase 2-E | Grafana + Tempo 분산 추적 (Waterfall Trace 확인) | ✅ |
| Phase 3 | AWS EC2/RDS 실배포 + CORS 전면 해결 | ✅ |

---

## 🔑 주요 기능

### 1. IAM — 인증/인가
- JWT Access + Refresh Token (Redis 저장)
- RBAC: SYS / ADMIN / DOC / NUR / LAB / RAD / PHARM / PROC
- 역할별 화면 접근 제한 (RoleGate 컴포넌트)

### 2. 접수 (adminMasterService)
- 예약/대기/응급 3탭 구조
- 예약내원 접수: RDS 예약 데이터 → 접수번호 자동생성
- 환자 정보 마스킹 (이름/주민번호/전화번호)
- 운영 제한 규칙: 최대 30명 (대기+진료중+예약+응급)

### 3. 진료 (clinicalService)
- SOAP 노트 작성 (S/O/A/P 4항목)
- 검사/영상/내시경 오더 다중 선택
- Kafka Outbox: 오더 완료 이벤트 → supportService 워크리스트 생성

### 4. 오더 (clinicalService)
- 최종처방: 약제/주사/수술/입원 복합 오더
- 실시간 비용 미리보기 자동 계산
- Kafka Saga: 오더확정 → 청구이벤트 → 수납서비스 연동

### 5. 수납 (adminMasterService)
- 최종오더 기반 영수증 자동생성
- 카드/현금 결제 처리
- 결제 완료 시 방문 상태 자동 COMPLETED 전환

### 6. 마스터 설정 (adminMasterService)
- 의사/원무 직원 프로필 CRUD
- RDS 실서버 동기화 (React Query invalidate)

---

## 📸 화면 스크린샷

### AWS 인프라 (실배포)
> EC2 t3.small + RDS MySQL 8.4 실배포. Security Group으로 포트별 접근 제어.

| EC2 인스턴스 | Security Group |
|:---:|:---:|
| ![aws-ec2](docs/screens/aws-ec2.png) | ![aws-security-group](docs/screens/aws-security-group.png) |

---

### 로그인 (JWT 실서버 인증)
> IAM 서비스에서 JWT 발급. 역할별 다계정 지원.

| system (SYS) | admin (ADMIN) | doctor (DOC) |
|:---:|:---:|:---:|
| ![login-system](docs/screens/login-system.png) | ![login-admin](docs/screens/login-admin.png) | ![login-doctor](docs/screens/login-doctor.png) |

---

### 대시보드
> RDS 실데이터 기반 환자 접수 현황 + 예약 현황. 도넛차트로 운영 인원 시각화.

| 로컬 상태 | 서버 동기화 후 |
|:---:|:---:|
| ![dashboard](docs/screens/dashboard.png) | ![dashboard-synced](docs/screens/dashboard-synced.png) |

---

### 접수 — 예약/대기/응급
> 예약내원 접수 시 RDS 예약 데이터 자동 조회. 환자 정보 마스킹 처리.

| 예약탭 | 대기탭 (예약선택) | 대기탭 (환자정보) | 접수목록 | 응급탭 |
|:---:|:---:|:---:|:---:|:---:|
| ![reception-reservation](docs/screens/reception-reservation.png) | ![reception-visit](docs/screens/reception-visit.png) | ![reception-visit-form](docs/screens/reception-visit-form.png) | ![reception-list](docs/screens/reception-list.png) | ![reception-emergency](docs/screens/reception-emergency.png) |

---

### 진료 — SOAP + 검사오더
> SOAP 노트 작성 후 검사/영상/내시경 오더 신청. Kafka Outbox로 지원서비스에 워크리스트 생성.

| SOAP 입력 | 검사오더 선택 |
|:---:|:---:|
| ![clinical-soap](docs/screens/clinical-soap.png) | ![clinical-order](docs/screens/clinical-order.png) |

---

### 오더 (최종처방)
> 약제/주사/수술/입원 복합 처방. 실시간 비용 미리보기 + 입원목록 조회.

| 처방 입력 | 비용 미리보기 + 입원목록 |
|:---:|:---:|
| ![orders-top](docs/screens/orders-top.png) | ![orders-bottom](docs/screens/orders-bottom.png) |

---

### 수납
> 최종오더 기반 영수증 자동생성 → 결제 완료 시 방문 상태 COMPLETED 전환.

| 오더 요약 | 영수증 생성 | 결제 완료 (PAID) |
|:---:|:---:|:---:|
| ![billing-summary](docs/screens/billing-summary.png) | ![billing-receipt](docs/screens/billing-receipt.png) | ![billing-paid](docs/screens/billing-paid.png) |

---

### 마스터 설정
> 직원 프로필 CRUD. RDS 실서버 동기화.

![master](docs/screens/master.png)

---

## ☁️ AWS 인프라

| 항목 | 사양 |
|---|---|
| EC2 | t3.small (2vCPU, 2GB) · ap-southeast-2 |
| RDS | MySQL 8.4 · db.t4g.micro |
| 컨테이너 | 9개 (gateway/iam/admin/clinical/support/kafka/zookeeper/redis/frontend) |
| 디스크 | 20GB EBS |
| 메모리 최적화 | Swap 2GB · 순차 빌드 (OOM 방지) |

### Security Group 인바운드
| 포트 | 용도 |
|---|---|
| 3000 | Frontend (Next.js) |
| 8180 | Gateway (Spring Cloud) |
| 22 | SSH |
| 3306 | MySQL (RDS) |

---

## 🚀 로컬 실행 방법

### 사전 요구사항
- Docker Desktop
- Java 17
- Node.js 18+

### 실행
```bash
# 프로젝트 루트
docker-compose up -d

# 프론트엔드 (로컬 개발)
cd csFrontend
yarn install
yarn dev
```

### 환경변수 (.env)
```env
MYSQL_HOST=localhost
MYSQL_USER=his_user
MYSQL_PASSWORD=his_password
GATEWAY_ALLOWED_ORIGIN=http://localhost:3000
NEXT_PUBLIC_GATEWAY_URL=http://localhost:8180
```

---

## 🧪 테스트

```bash
# JUnit5 단위 테스트
cd csBackend/adminMasterService
./gradlew test

# GitHub Actions CI (자동 실행)
# .github/workflows/ci.yml
```

---

## 📊 Kafka Choreography Saga 흐름

```
[접수] Visit 등록
    │
    ▼ (Outbox → Kafka)
[진료] SOAP 작성 + 검사오더
    │
    ▼ (Outbox → Kafka)
[지원] 워크리스트 생성 (LAB/RAD/PHARM)
    │
    ▼ (결과 등록 후 Outbox → Kafka)
[오더] 최종처방 확정
    │
    ▼ (Outbox → Kafka: BillingRequested)
[수납] 영수증 자동생성 → 결제 → COMPLETED
```

---

## 📁 프로젝트 구조

```
hospitalMSA/
├── csBackend/
│   ├── gatewayService/      # Spring Cloud Gateway + JWT Filter
│   ├── iamService/          # 인증·인가 (JWT + Redis + RBAC)
│   ├── adminMasterService/  # 접수·수납·마스터 (+ Kafka Consumer)
│   ├── clinicalService/     # 진료·오더 (+ Kafka Outbox Publisher)
│   └── supportService/      # 검사지원 (LAB/RAD/PHARM/PROC)
├── csFrontend/              # Next.js 14 App Router
│   └── src/
│       ├── app/             # 라우팅
│       ├── features/        # 화면별 컴포넌트
│       └── shared/          # 공통 (store/services/components)
├── docker-compose.yml       # 9개 컨테이너 구성
└── .github/workflows/       # GitHub Actions CI
```

---

## 🔗 브랜치 전략

| 브랜치 | 내용 |
|---|---|
| `master` | Phase 1 — MySQL full-stack 기본 |
| `feature/phase2-oracle` | Phase 2 — Oracle 포팅 + React Query + Kafka + JUnit5 + Grafana |
| `feature/phase3-aws` | Phase 3 — AWS EC2/RDS 실배포 (현재) |

---

> 개발자: Park Su Beom  
> 목표: 토스뱅크 / 카카오페이 백엔드 엔지니어
