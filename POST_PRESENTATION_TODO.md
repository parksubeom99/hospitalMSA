# 발표 후 작업 리스트 v2 — Claude Code 인계용

**기록일**: 2026-04-24 (v2.2: 2026-04-24 S-2 부분 완료 반영 — PR #2 머지)
**목표**: 면접 / 이력서 / 포트폴리오 완성도 향상
**실행 주체**: Claude Code
**대상 리포지토리**: `parksubeom99/hospitalMSA` (master)
**기반 HEAD**: `81553f3` (BFG `git filter-repo` 등가 처리 후 — 구 `224aebe` 재작성)
**SHA 재작성 영향**: 본 문서 매핑 표의 SHA는 BFG 후 새 SHA로 갱신됨. 옛 SHA(`224aebe`, `386bdad` 등)는 GitHub에서 더 이상 접근 불가(404)

---

## 본 문서의 근거 수립 방법

모든 항목은 아래 세 가지 근거 중 최소 하나를 갖는다:

- **실측**: 본 리포지토리에서 직접 grep / find / wc로 측정한 수치
- **커밋 이력**: 발표 전 실제 발생해 수정된 커밋 SHA + 에러 재현 경로
- **로그 증거**: `clinical_log.txt` 등 실제 발생 스택 트레이스

숫자 기재 시 실측 기준을 우선한다. (기존 TODO v1의 "Controller 47개"는 실측 43개로 정정됨)

---

## Claude Code 실행 규칙 (공통)

1. 각 항목은 **독립 브랜치 + 독립 PR**로 처리한다. 브랜치명: `feature/post-pres-<번호>-<핵심키워드>`
2. PR 본문에는 반드시 아래를 포함한다:
   - "왜 이 작업을 하는가" (본 문서의 근거 섹션 복사)
   - "어떻게 검증했는가" (재현 경로 또는 테스트)
   - 관련 커밋 SHA (이 문서의 "실제 터진 오류" 섹션 참조)
3. 커밋 메시지 규약: `<type>(<scope>): <제목>` — type은 feat / fix / refactor / test / chore / docs / security
4. 각 항목 완료 시 이 문서의 체크박스 `[ ]` → `[x]` 갱신 후 별도 docs 커밋

---

# 우선순위 S급 — Week 1 (면접 안전망)

> S급은 **면접 필터링 리스크**를 직접 차단한다. 공수 대비 효과 최고.

---

## S-1. `token.txt` Git 이력 완전 삭제 (보안 최우선) ✅ **완료 (2026-04-24)**

**우선순위**: 🔴 최우선 (PR #1 이미 merge됨 → force push 안전)

### 완료 보고

- 도구: **BFG Repo-Cleaner 1.14.0** (Java 기반, `filter-repo` 대체 — 노트북에 Python 부재)
- 백업: `C:\dev\potpolio\hospitalproject.backup.20260424` (31M)
- 결과: 37 객체 변경. master `224aebe → 81553f3`, phase2-oracle `310c5d4 → 14846bf`
- 검증:
  - 로컬·원격 `git log --all -- token.txt` 결과 **0건**
  - GitHub `raw.githubusercontent.com/.../master/token.txt` → **404**
  - 옛 커밋 `386bdad` 페이지 → **404**
- 부수 효과: 본 문서의 모든 옛 SHA 무효화 → 매핑 표 갱신함

### 왜 이 작업을 했는가 (원본)

- `git log --all -- token.txt` 결과: **커밋 2개**에 토큰 파일 이력 남음
  - `386bdad fix: Reception WAITING 상태만 수정/삭제 허용` (2026-04-16 최초 유입)
  - `8bd4e2d chore(security): token.txt 추적 제거` (2026-04-20 추적만 해제, 이력은 잔존)
- 커밋 `8bd4e2d` 본문 일부 (**당시 판단의 한계**):
  - > "JWT 토큰은 이미 만료되어 즉각적 탈취 위험은 없지만... 발표 전까지는 추적 제거만 수행"
- **실제 리스크 (정정)**:
  - HS256 JWT가 공개되면 **서명 시크릿 무차별 대입 공격** 가능
  - 토스/카카오 채용 담당자가 리포 확인 시 "보안 의식 부족" 판정 → **면접 필터링 1순위 지표**
  - 토큰 만료 여부와 무관하게 **"커밋한 사실 자체"**가 평가 대상
- 발표 전에는 PR #1 머지를 위해 미뤘으나, 지금은 PR #1 이미 merge 완료 → **force push 안전 타이밍**

### 체크리스트

- [x] ~~`git filter-repo` 설치~~ → **BFG 1.14.0 (jar)** 사용 (Python 부재 환경 대응)
- [x] 현재 리포 전체 백업 (`hospitalproject.backup.20260424`)
- [x] `java -jar bfg.jar --delete-files token.txt <repo>` 실행
- [x] `git log --all -- token.txt` 결과 0건 확인
- [x] `git push origin --force --all` 실행 (모든 브랜치)
- [x] `git push origin --force --tags` 실행 (로컬 태그 0개 — no-op)
- [x] GitHub 측 raw URL + 옛 커밋 SHA 페이지 404 확인
- [ ] 로컬 `token.txt` 파일을 `.env.local` 등 무관 경로로 이동 (개발 사용성 유지) — **선택 사항**, gitignore로 차단되어 안전
- [x] `.gitignore`에 이미 `token.txt` / `*.token` 패턴 있는지 재확인 (이미 등록됨)

### 완료 기준

- [x] GitHub 웹에서 `token.txt` 접근 시 **404** (raw URL + 옛 SHA 커밋 페이지)
- ~~로컬 `git log --all --oneline | wc -l`이 24 → 22 이하로 감소~~ — **잘못된 기준** (BFG/filter-repo는 커밋을 재작성할 뿐 개수를 줄이지 않음. 본 작업 후 26개 그대로 유지)

---

## S-2. Gateway OpenTelemetry 계측 추가 (관측성 완결) 🟡 **부분 완료 (2026-04-24, PR #2 머지)**

**우선순위**: 🔴 최우선 (README/PPT의 분산 추적 스토리 완성)

### 완료 보고 (PR #2 — `6ee695d`)

- 도구: **Micrometer Tracing 패턴** (admin/clinical과 동일) — v2 원안의 javaagent 방식 대신 채택
- 추가 의존성: `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`
- 설정 파일: `csBackend/gatewayService/src/main/resources/application.yml`
- 검증: Grafana Tempo Service Name 드롭다운에 `gateway-service` 등장 ✅
- 부수 가치: `/api/admin/dashboard/summary` 요청 시 **3-span trace 자동 생성**:
  - `gateway-service: http get` (2.54ms)
  - └ `security filterchain before` (1.74ms) — JWT 인증 병목 가시화
  - └ `security filterchain after` (165µs)
- 캡처: `docs/image/observability/gateway-otel-tempo-3span.png`

### 부분 완료인 이유 — 별 PR(S-2-B)로 분리된 작업

작업 중 **두 가지 별개 이슈** 발견:
1. Reactor Netty downstream client에 `traceparent` 헤더 전파 미구현 → cross-service trace 단절
2. **admin/clinical도 HTTP server span을 생성하지 않음** (Kafka publisher span만) — Phase 2-E 시점부터 잠재했던 문제

→ 본 PR은 Gateway 측 OTel 추가까지만 마무리. Cross-service waterfall은 S-2-B 후속 PR로 분리.

### 왜 이 작업을 하는가

- **실측 grep 결과 (2026-04-24 재측정)**:
  - `grep -l opentelemetry csBackend/*/src/main/resources/application*.properties` → **2개 서비스(admin/clinical)** 만 설정 ⚠ (v2 초안의 "4개" 주장은 오류 — iam/support는 properties 파일에 `opentelemetry` 키워드 자체가 없음)
  - `grep -E "OTEL|otel" docker-compose-oracle.yml` → **0건** (Gateway 환경변수 전무)
  - `cat csBackend/gatewayService/src/main/resources/application.yml` → CORS/라우트만 있고 tracing 없음
  - **재해석**: 현재 OTel 적용은 **2/5(40%)** 수준. Gateway뿐 아니라 iam/support도 향후 OTel 확장 후보 (S-2 본 작업은 Gateway에 한정)
- **현재 상태의 한계**:
  - Grafana Tempo Service Name 드롭다운에 `gateway-service` 미표시
  - README의 "Admin Service HTTP Waterfall"에 **Gateway → Admin 2-span 체인이 아닌 Admin 단독 1-span**만 표시됨
- **면접에서의 약점**:
  - 면접관 "end-to-end 분산 추적 보여주세요" 요청 시 Gateway 스팬 부재를 설명해야 함
  - 현재 답변 라인: "Gateway는 추후 계측 예정" → **변명형**
  - 작업 후 답변 라인: "브라우저 → Gateway → Admin까지 트레이스로 모두 확인 가능" → **증명형**

### 체크리스트

- ⚪ ~~`docker/otel/opentelemetry-javaagent.jar` 파일 존재 확인~~ — **무효**: javaagent 대신 Micrometer 패턴 채택
- ⚪ ~~`docker-compose-oracle.yml` env 추가~~ — **무효**: 동일 이유
- ⚪ ~~`gateway-service` 블록 volumes 추가~~ — **무효**: 동일 이유
- ⚪ ~~docker-compose 주석 추가~~ — **무효**: docker-compose 변경 자체 없음
- [x] `docker compose -f docker-compose-oracle.yml build gateway-service` 성공 (PR #2)
- [x] `docker compose up -d` 후 gateway 정상 기동 (3.3초) — `docker logs psb-gateway` 확인
- [x] Grafana → Explore → Tempo → Service Name 드롭다운에 `gateway-service` 등장 확인 ✅
- [ ] 브라우저 → `/admin/dashboard/summary` → Tempo Waterfall에 **Gateway → Admin 2-span chain** — 🔄 **S-2-B PR로 이관**
- [x] README 분산 추적 섹션에 캡처 추가 (gateway-otel-tempo-3span.png — 6번째 자료)

### 완료 기준

- [x] Grafana에서 `http get /api/admin/dashboard/summary` 트레이스 클릭 시 `gateway-service` 스팬이 루트로 표시 ✅ (3-span 중첩 trace 확인)
- [ ] 총 span 수 2개 이상 (Gateway 1 + Admin 1) — 🔄 **S-2-B 이관**: 현재 gateway 단독 trace 3-span. Cross-service chain은 (a) Reactor Netty traceparent 전파 + (b) admin/clinical HTTP server span 활성화 두 작업 필요

---

# 우선순위 A급 — Week 2 (면접 약점 봉합)

> A급은 **면접관이 직접 질문할 가능성이 높은 약점**을 코드로 방어한다.

---

## A-1. 테스트 커버리지 확장 (2파일 → 5파일 / 8케이스 → 15케이스)

**우선순위**: 🟠 높음 (면접 약점 1순위)

### 왜 이 작업을 하는가

- **실측 grep 결과**:
  - `find csBackend -path "*/test/*" -name "*Test*.java"` → **3개 파일**
    - `VisitServiceTest.java` (adminMasterService)
    - `VisitRegisteredConsumerTest.java` (clinicalService)
    - `IamApplicationTests.java` (context loading만)
  - `grep -rE "^\s*@Test\b" csBackend/*/src/test/` → **@Test 8개**
  - `find csBackend -name "*Controller.java"` → **43개**
- **커버리지 계산**: 8 @Test / 43 Controller ≈ **19%** (빅테크 최소 기준 60%)
- **면접에서의 취약점**:
  - 면접관 "테스트 커버리지 얼마입니까?" 한 질문으로 방어 붕괴
  - POST_PRESENTATION_TODO v1의 답변 라인 "핵심 Saga 체인 우선 커버" = **변명형**
  - 솔직 답변: "시간 제약으로 핵심 Saga Consumer 집중, 발표 후 15케이스로 확장 완료" = **실행형** (단, 실제로 확장해야만 말할 수 있음)

### 체크리스트 (우선순위 순)

- [ ] `BillingRequestedConsumer` 테스트 신규 작성 (2~3 @Test)
  - 근거: Saga 체인의 **수납 진입점** → 면접 Q&A 가장 자주 질문
  - 시나리오: `BILLING_REQUESTED` 이벤트 수신 → `InvoiceService` 호출 → `BILLING_COMPLETED` 발행
- [ ] `BillingCompletedConsumer` 테스트 신규 작성 (2 @Test)
  - 근거: Saga 체인의 **완료 수신부** → Compensation 로직과 연결
- [ ] `DiagnosticOrderCompletedConsumer` 테스트 신규 작성 (2 @Test)
  - 근거: 진단 오더 체인 완결성
- [ ] `ReservationService.checkIn` 시나리오 테스트 추가 (1~2 @Test)
  - 근거: 커밋 `77cb1ef`에서 "예약 → 접수 시 status=CHECKED_IN + Visit 자동 생성" 핵심 버그 수정
  - 회귀 방지 목적
- [ ] `@Test` 총 **15개 이상** 달성 확인 (`grep -rE "^\s*@Test\b" csBackend/*/src/test/ | wc -l`)
- [ ] GitHub Actions CI 통과 확인 (초록불)
- [ ] README "테스트 현황" 섹션 숫자 갱신 (현재 "7 케이스" → 실측값)

### 완료 기준

- 테스트 파일 5개 이상, @Test 15개 이상
- CI Green 유지

### 실제 터진 관련 오류 (참고용)

- 커밋 `310c5d4 test: VisitService/VisitRegisteredConsumer 테스트 — 최근 시그니처 변경 반영`
  - 증상: `VisitClinicalStatusService.initOrGet(Long)` → `initOrGet(Long, String)` 시그니처 변경 후 기존 테스트가 컴파일 깨짐
  - **교훈**: 테스트가 적으면 시그니처 변경의 영향 범위를 실시간으로 감지 못 함. 확장 시 Mock 인자를 **null 허용 오버로딩**으로 방어할 것

---

## A-2. Saga 플로우 명시적 문서화 클래스 추가

**우선순위**: 🟠 높음 (면접 코드 네비게이션 증거)

### 왜 이 작업을 하는가

- **실측 grep 결과**:
  - `find csBackend -name "*Saga*.java"` → **0건**
  - `find csBackend -name "*Consumer.java"` → **8개** (실제 Choreography 로직은 존재)
- **면접에서의 취약점**:
  - 면접관 "Saga 오케스트레이터 어디 있나요?" → Choreography 개념 설명만으로는 **시각적 증거 부족**
  - IDE에서 프로젝트 열자마자 `Saga` 파일명으로 grep 실패 → **탐색성 0**
- **목표**: Choreography 철학은 유지하되, 이벤트 흐름을 **문서화용 상수 클래스**로 집약

### 체크리스트

- [ ] `adminMasterService`에 `kr.co.seoulit.his.admin.saga.VisitBillingSagaFlow.java` 생성
  - 내용: JavaDoc에 전체 플로우 다이어그램(텍스트) + 이벤트 상수 8개
    ```java
    /**
     * Visit → Billing Saga (Choreography)
     *
     * 1. Admin.VisitService.create          → VISIT_REGISTERED 발행
     * 2. Clinical.VisitRegisteredConsumer   → visit_clinical_status WAITING
     * 3. Clinical.SoapService.save          → (SOAP 저장)
     * 4. Clinical.OrderService.send         → DIAGNOSTIC_ORDER_REQUESTED
     * 5. Support.OrderConsumer              → 검사 결과 발행
     * 6. Clinical.DiagnosticOrderCompleted  → EXAM_COMPLETED
     * 7. Admin.QueueEventConsumer           → BILLING_REQUESTED
     * 8. Admin.BillingRequestedConsumer     → 청구 생성 → BILLING_COMPLETED
     * 9. Clinical.BillingCompletedConsumer  → visit_clinical_status COMPLETED
     *
     * Compensation: BILLING_FAILED 발행 시 markBillingFailed()로 롤백
     *               (자동 복구는 TODO A-3 참조)
     */
    public final class VisitBillingSagaFlow {
        public static final String EVT_VISIT_REGISTERED = "VISIT_REGISTERED";
        public static final String EVT_DIAGNOSTIC_ORDER_REQUESTED = "DIAGNOSTIC_ORDER_REQUESTED";
        public static final String EVT_EXAM_COMPLETED = "EXAM_COMPLETED";
        public static final String EVT_BILLING_REQUESTED = "BILLING_REQUESTED";
        public static final String EVT_BILLING_COMPLETED = "BILLING_COMPLETED";
        public static final String EVT_BILLING_FAILED = "BILLING_FAILED";
        // ... 등
        private VisitBillingSagaFlow() {}
    }
    ```
- [ ] `clinicalService`에 동일 클래스 생성 (관점은 Clinical 기준)
- [ ] 기존 Consumer들이 literal 문자열(`"VISIT_REGISTERED"`) 대신 **상수 참조**로 마이그레이션 (선택)
- [ ] README ADR-001에 "SagaFlow 문서화 클래스로 탐색성 보강" 한 줄 추가

### 완료 기준

- `grep -r "VisitBillingSagaFlow" csBackend/` 결과 2개 이상 서비스에서 import 또는 JavaDoc 확인
- IDE에서 "SagaFlow"로 검색 시 즉시 진입점 발견

### 주의사항

- **오케스트레이터가 아님**. 이 클래스는 실행 로직 0 / 상수+JavaDoc만 가진다. 면접에서 "이건 오케스트레이터인가요?" 질문 대비 답변: "아니요, Choreography의 이벤트 이름을 일관되게 관리하고 플로우를 문서화하기 위한 것입니다."

---

## A-3. 자동 Compensation 트랜잭션 (하이브리드 보상)

**우선순위**: 🟠 높음 (면접 Q "자동 롤백은?" 대응)

### 왜 이 작업을 하는가

- **실측 확인**:
  - `VisitClinicalStatusService.markBillingFailed` 메서드 존재 → 현재는 visit을 `BILLING_FAILED` 상태로 전환 후 **담당자 수동 재처리 대기**
  - 완전 자동 Compensating Transaction 아님
- **면접에서의 취약점**:
  - 면접관 "결제 실패 시 자동 롤백 어떻게 되나요?" → 현재 답변은 "수동 처리" → **MSA Saga 설계 이해도 의심받음**
- **목표**: 도메인 맥락 반영한 **조건부 자동 + 수동 하이브리드 보상**
  - 완전 자동은 의료 업무 현실과 안 맞음 (청구 실패가 진료 취소를 의미하지 않음)
  - 조건부 분기로 "적절한 자동화"를 구현했다는 답변 라인 확보

### 체크리스트

- [ ] 공통 추상화: `SagaCompensationHandler` 인터페이스 작성 (clinical/saga 패키지)
  - 메서드: `boolean canAutoRecover(Event e)`, `void compensate(Event e)`
- [ ] `BillingFailedCompensationHandler` 구현 클래스 작성
  - 분기 규칙:
    - `visit.status == COMPLETED` && `invoice == null` → **자동** BILLABLE 복귀 (재청구 가능)
    - `invoice != null && invoice.status == UNPAID` → **자동** 단순 상태 롤백
    - 그 외 → `markBillingFailed` 호출 (수동 개입 유지)
- [ ] `BillingCompletedConsumer`와 대칭으로 `BillingFailedConsumer` 신규 작성
  - Kafka topic: `BILLING_FAILED`
  - 수신 시 Handler 호출
- [ ] A-1 테스트 확장에 `BillingFailedCompensationHandlerTest` 2 @Test 추가
  - 케이스 1: 자동 복구 성공 → visit.status == BILLABLE
  - 케이스 2: 수동 개입 필요 → visit.status == BILLING_FAILED

### 완료 기준

- BILLING_FAILED 이벤트 수신 → 조건별 분기 실행 검증 (테스트 포함)
- 면접 답변 라인 확보: "자동/수동 분기 보상. 도메인 맥락 반영한 하이브리드"

---

# 우선순위 B급 — Week 3 (설계 결함 정리)

> B급은 **실제 발표 중 터진 버그의 근본 원인**이다. 면접 질문 대비보다 엔지니어링 품질 증명.

---

## B-1. HospitalStore ↔ 서버 상태 일원화

**우선순위**: 🟡 중간 (실제 버그 3건의 근본 원인)

### 왜 이 작업을 하는가

- **실제 터진 오류 (커밋 `b290967`)**:
  - 증상 1: 서버 시드 리셋 후 0건 응답에도 브라우저가 **localStorage 이전 데이터**로 유령 화면 표시
  - 증상 2: 로컬 `reservations` 배열(21001~21003)이 **서버와 분리된 유령**으로 UI에 잔류 → 취소/체크인 시 404
  - 커밋 메시지 인용: "로컬 시드 fallback 제거 — 서버 0건도 신뢰"
- **실제 터진 오류 (커밋 `77cb1ef`)**:
  - 증상: 예약내원 접수 자동 채움 시 `state.reservations`(빈 로컬) 참조 → 서버 예약 선택해도 빈 폼
  - 수정: `reservationsQuery.data` 우선 참조로 전환
- **근본 원인**: 로컬 HospitalStore와 React Query 서버 상태가 **이중 소스(dual source of truth)**로 공존
- **면접에서의 취약점**:
  - 면접관 "서버 상태 관리 라이브러리 왜 넣었나?" → "React Query 넣었습니다" 답변 가능
  - 하지만 코드에서는 `state.reservations`와 `reservationsQuery.data` 혼용 → **부분 적용**임이 드러남
  - 일원화 후 답변: "React Query가 모든 서버 데이터의 단일 진실 공급원. 로컬 Store는 UI/세션 상태 전용"

### 체크리스트

- [ ] 삭제 대상 식별: `HospitalStore`의 서버 데이터 필드 (`patients`, `reservations`, `visits`, `soaps`, `orders` 등)
- [ ] 각 Screen 파일에서 `state.patients` / `state.reservations` / `state.visits` / `state.soaps` 참조 grep
- [ ] 모든 참조를 `patientsQuery.data` / `reservationsQuery.data` 등으로 교체
- [ ] `HospitalStore` 타입 정의에서 서버 데이터 필드 **제거**
- [ ] 시연 모드(데모초기화) 필요 시: 별도 slice `DemoSampleStore` 분리 (읽기 전용)
- [ ] 브라우저 E2E 수동 확인:
  - (a) 로그인 → 접수 등록 → 예약 현황 즉시 표시
  - (b) 시드 리셋 → 강력 새로고침 → 화면 0건 (이전 localStorage 잔재 없음)
  - (c) 예약내원 드롭다운 선택 → 환자명/전화 자동 채움
- [ ] `ReceptionScreen.tsx` 주석의 `// [FIXED v3.2]` 블록 다시 리뷰 — 일원화 후 불필요해진 것 삭제

### 완료 기준

- `grep "state\.reservations\|state\.visits\|state\.patients\|state\.soaps" csFrontend/src/` 결과 0건 (또는 세션/UI 용도만 남음)
- 위 3가지 수동 테스트 모두 통과

---

## B-2. 임시 `patientId = Date.now()` 구조 개선

**우선순위**: 🟡 중간 (데이터 쓰레기 축적 방지)

### 왜 이 작업을 하는가

- **실측 grep 결과** (`ReceptionScreen.tsx`):
  ```tsx
  if (serverWriteEnabled) {
    const tempPatientId = Date.now();   // ← 타임스탬프를 ID로 사용
    await upsertPatientForReception({
      session: state.session ?? undefined,
      patientId: tempPatientId,
      name: reservationForm.name,
      gender: "M",
      rrnFront: "000000",
      rrnBack: "1000000",
      ...
    });
  }
  ```
- **결과**:
  - `patient` 테이블에 `patient_id = 1.7766E+12` 같은 임시 환자가 계속 누적
  - 시드 리셋 SQL에서 매번 정리 필요 (발표 당일 아침 루틴의 일부)
- **발표 중 실제 영향**:
  - 리허설 잔재 누적 문제 (메모리의 2026-04-22~23 회고 참조)
  - 운영 환경에서는 **데이터 쓰레기 → 구조적 결함**
- **면접에서의 취약점**:
  - 코드 리뷰 시 `Date.now()`를 ID로 쓴 것 발견 시 "실 서비스 고려 부족" 판정

### 체크리스트

- [ ] 백엔드: `adminMasterService`에 엔드포인트 추가
  - `POST /admin/patients/upsert-by-name-phone`
  - Request: `{ name, phone, gender, rrnFront, rrnBack }`
  - Response: `{ patientId, name, isNew }` (기존 환자면 `isNew: false`)
  - 구현: Repository에서 `findByNameAndPhone` → 존재 시 기존 반환, 없으면 신규 생성
- [ ] 프론트엔드: `receptionMutationApi.ts`에 `upsertPatientByNamePhone` 함수 추가
- [ ] `ReceptionScreen.tsx`에서 `Date.now()` 사용 지점 제거 → 새 API 응답의 `patientId` 사용
- [ ] 테스트: 동일 이름+전화로 두 번 등록 → 두 번째는 기존 patientId 반환 확인
- [ ] 시드 리셋 SQL에서 "임시 환자 삭제" 조항 제거 가능 (`1.7766E+12` 같은 대형 ID 청소 로직)

### 완료 기준

- `grep "Date.now()" csFrontend/src/features/reception/` 0건
- 시드 리셋 없이 접수 2회 반복해도 `patient` 테이블 누적 없음

---

## B-3. DatePicker 교체 (UX)

**우선순위**: 🟡 중간 (시연 영상 촬영 시 효과)

### 왜 이 작업을 하는가

- **현재 상태**: `ReceptionScreen.tsx`에서 브라우저 네이티브 `<input type="datetime-local">` 사용
- **회장님 직접 지적 사항** (발표 준비 세션에서):
  - "선택완료 버튼 없음"
  - "시·분 무한 스크롤"
  - UX 품질 저하 → 시연 영상 시청자에게 "덜 다듬어진" 인상
- **면접 이력서 링크용 영상 촬영 시 임팩트 개선**

### 체크리스트

- [ ] `cd csFrontend && npm install react-datepicker @types/react-datepicker`
- [ ] `ReceptionScreen.tsx`에서 `<input type="datetime-local">` 3곳 grep → `<DatePicker>` 교체
- [ ] 한글 locale 설정 (`import { registerLocale } from "react-datepicker"; import ko from "date-fns/locale/ko";`)
- [ ] `showTimeSelect`, `timeFormat="HH:mm"`, `timeIntervals={15}` 설정 (유한 시·분 선택)
- [ ] `dateFormat="yyyy-MM-dd HH:mm"` 설정
- [ ] 다크모드 호환 확인 (기존 Tailwind 테마와 충돌 시 커스텀 CSS)
- [ ] 브라우저 수동 확인: 예약 등록 폼에서 DatePicker 선택 → 완료 버튼 → 폼 반영

### 완료 기준

- 3곳 모두 DatePicker로 전환
- 시·분 15분 단위 드롭다운 표시 확인

---

# 우선순위 C급 — Week 4+ (확장)

> C급은 **여유 있을 때 진행**. Toss보다는 Kakao 지원 자격에 직접 영향.

---

## C-1. K8s / Helm 도입 (ECS → EKS 2트랙)

**우선순위**: 🟢 확장 (Kakao 지원 필수 역량)

### 왜 이 작업을 하는가

- **현재 상태**: Docker Compose 12 컨테이너 (Phase 3 AWS EC2에서도 동일)
- **MSA 본질 증명**: Docker Compose로 충분 ✅
- **면접 리스크**:
  - Toss 1순위는 K8s 필수 아님
  - 그러나 **Kakao Pay / Mobility는 사실상 필수** (JD 공통 요구사항)
  - 면접관 "EKS 운영 경험 있나요?" 직접 질문 빈발
- **목표**: AWS ECS (입문) → EKS (본격) → Helm (재사용) 순차 진행

### 체크리스트

- [ ] **Step A — ECS (Fargate)**: Compose 파일을 ECS Task Definition으로 변환
  - `ecs-cli compose convert` 또는 수동 작성
  - Terraform 또는 CloudFormation으로 IaC
  - 배포 후 트래픽 테스트 1회
- [ ] **Step B — EKS (Kubernetes)**:
  - `eksctl create cluster` 또는 Terraform
  - 각 서비스를 Deployment / Service / Ingress YAML로 작성 (5개 서비스 × 3 YAML = 15개)
  - ConfigMap / Secret 분리 (DB 비밀번호, JWT 시크릿)
  - HPA (HorizontalPodAutoscaler) 설정 (CPU 기준)
- [ ] **Step C — Helm**:
  - 5개 서비스를 하나의 Helm Chart로 패키징
  - `values.yaml`에 환경별(dev/prod) 설정 분리
  - `helm install hospitalmsa ./chart` 1커맨드 배포
- [ ] README에 "Phase 4 K8s" 섹션 추가 + 아키텍처 다이어그램 갱신
- [ ] 면접 답변 라인 작성: "MSA 본질은 Compose에서 검증, K8s는 Ops 레이어로 후순위였음. Phase 4에서 EKS + Helm 확장 완료"

### 완료 기준

- EKS 클러스터에 5개 서비스 전부 배포 + 브라우저 접속 가능
- Helm 1커맨드로 전체 배포 가능

---

## C-2. 프론트엔드 커버리지 확장

**우선순위**: 🟢 확장 (공수 대비 면접 방어력 낮음 — 시간 여유 있을 때)

### 왜 이 작업을 하는가

- **현재 상태**: Controller **43개** vs 프론트 주요 화면 **8개** (접수/진료/오더/수납/대시보드/마스터/로그인/감사로그)
- **면접 답변 라인 유지**: "핵심 업무 흐름(접수→진료→수납) 완결성 우선, 부가 화면은 선택적 확장"
- **본 확장의 목적**: 이력서 영상 길이 확보 + "풀스택 역량" 강화

### 체크리스트 (우선순위 순)

- [ ] **환자 상세 조회 화면**: `/admin/patients/{id}` 기반
  - 인구통계 / 방문 이력 / 오더 이력 / 청구 이력 탭
- [ ] **감사로그 필터·검색 확장**: 기간 / 사용자 / 이벤트유형 필터
  - 현재 텍스트 리스트만 있음 → `DataGrid` 스타일로 개선
- [ ] **청구 상세 화면**: invoice item breakdown + adjustment 내역
- [ ] **예약 캘린더 뷰**: `react-big-calendar` 또는 `fullcalendar`로 월간/주간 마킹
- [ ] **Admin 대시보드 위젯 추가**: 시간대별 방문 그래프 (Chart.js), 의사별 방문 건수 bar chart

### 완료 기준

- 5개 중 최소 3개 완료 → 화면 개수 8 → 11+
- 이력서 영상 촬영 시 "풀스택 일관성" 표현 가능

---

## C-3. CI Node 24 업그레이드

**우선순위**: 🟢 확장 (2026-06-02 강제 전환 전)

### 왜 이 작업을 하는가

- **현재 상태** (`.github/workflows/ci.yml`):
  - Node 20 actions 사용 중
  - CI 실행마다 "Node.js 20 actions are deprecated" warning
- **타임라인 (GitHub 공지)**:
  - **2026-06-02** 강제 전환
  - **2026-09-16** Node 20 runner 완전 제거

### 체크리스트 (3가지 중 택1)

- [ ] **A. 임시 환경변수** (최소 공수):
  ```yaml
  env:
    FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: "true"
  ```
- [ ] **B. Actions 버전 업그레이드** (권장):
  - `actions/checkout@v4` → 최신
  - `actions/setup-java@v4` → 최신
  - `actions/cache@v4` → 최신
  - `actions/upload-artifact@v4` → 최신
  - 각 변경 후 CI 1회 Green 확인
- [ ] **C. Self-hosted runner**: 개인 프로젝트엔 과함 — **채택 안 함**

### 완료 기준

- CI 실행 로그에 `Node.js 20 actions are deprecated` warning 0건

---

# 실제 터진 오류 → TODO 항목 매핑 표

이 섹션은 **"왜 이 체크리스트가 필요한가"에 대한 최종 근거**이다. 발표 전 실제 발생해 수정된 오류들이 어느 TODO와 연결되는지 한눈에 본다.

> ⚠️ **SHA 갱신 안내 (2026-04-24)**: S-1 BFG 작업으로 `token.txt` 영향 받은 모든 커밋의 SHA가 재작성됨. 아래 표는 **신 SHA → 구 SHA(참고)** 형식으로 갱신.

| 신 SHA (현재) | 구 SHA (BFG 전) | 증상 | 수정 내용 | 연관 TODO | 메모 |
|---|---|---|---|---|---|
| `76165f1` | `4715544` | Gateway API 전부 401 | WebFlux Security + CORS 정상화 | S-2 (연장선) | OTel도 같은 파일 영역이므로 재빌드 비용 공유 |
| `3c66d96` | `b290967` | 서버 0건 응답에도 유령 데이터 표시 | 로컬 시드 fallback 제거 | **B-1 근본 원인** | 일원화 안 하면 재발 |
| `6c9de6a` | `f6b88d1` | 대기 목록 성별 컬럼 undefined | Patient JOIN으로 응답 확장 | A-1 (회귀 테스트) | VisitResponse DTO 시그니처 변경 사례 |
| `03d50d6` | `77cb1ef` | 예약내원 드롭다운 자동 채움 실패 | reservationsQuery.data 우선 참조 | **B-1 근본 원인** | 이중 소스 혼용 문제 |
| `03d50d6` | `77cb1ef` | 예약 현황에 접수 후에도 잔류 | checkInReservationServer 호출 추가 | A-1 (회귀 테스트) | checkIn 시나리오 테스트 대상 |
| `773df62` | `ea15d26` | 진료 드롭다운 환자명 "환자"로 폴백 | visit_clinical_status.patient_name 영속화 | A-1 (Consumer 테스트) | VisitRegisteredConsumer 시그니처 변경 |
| `fda0744` | `386bdad` | token.txt 커밋 유입 (BFG로 token.txt만 제거됨) | WAITING 상태만 수정/삭제 허용 (부산물로 토큰 노출) | **S-1 ✅ 완료** | 신 SHA에는 token.txt 없음 |
| `802dc15` | `8bd4e2d` | token.txt 추적 해제만 함 (이력 잔존) | .gitignore + git rm --cached | **S-1 ✅ 완료** | BFG로 이력 완전 제거 |
| `15cb364`, `1d89d6e`, `4845792` | `2d6b5e2`, `e96fe9d`, `fa0ea00` | CI 파이프라인 실패 | gradlew 실행 권한 + wrapper.jar 예외 + 누락 wrapper | C-3 (연장선) | Actions 버전 업 시 재발 방지 필요 |
| Flyway 실패 (clinical_log.txt) | — | `Schema "HIS_CLINICAL" contains a failed migration to version 2 !` | 수동 `UPDATE flyway_schema_history` 복구 | **운영 교훈** (README ADR에 반영) | 같은 오류 재발 시 대응 SQL 문서화 |

---

# Claude Code 인계 체크리스트 (최종)

작업 진입 전 아래를 순서대로 확인:

- [x] 이 문서를 레포 루트에 `POST_PRESENTATION_TODO.md`로 커밋 (기존 v1 대체) — 2026-04-24
- [x] `git pull origin master` 실행 (발표 이후 추가 커밋 있는지 확인) — BFG 직후 동기화 완료
- [ ] 우선순위 S → A → B → C 순서 고수 (건너뛰기 금지)
- [ ] 각 항목마다 독립 브랜치 + PR
- [ ] PR 생성 전 **해당 항목의 "완료 기준" 모두 체크된 상태**에서만 PR 오픈
- [ ] Codex / CodeRabbit 등 PR 리뷰 봇 지적은 P1/P2는 반드시 검토 후 대응
- [x] 완료된 항목은 이 문서 체크박스 `[ ]` → `[x]` 갱신 + `docs:` 커밋 — S-1 본 커밋에서 적용

---

# 참고: 발표 후 숫자 정정 (v1 → v2)

| 항목 | v1 기재 | 실측 v2 | 근거 |
|---|---|---|---|
| Controller 수 | 47개 | **43개** | `find csBackend -name "*Controller.java" \| wc -l` |
| 테스트 파일 | 2개 | **3개** (IamApplicationTests 포함) | `find csBackend -path "*/test/*" -name "*Test*.java"` |
| @Test 케이스 | 7개 | **8개** | `grep -rE "^\s*@Test\b" csBackend/*/src/test/ \| wc -l` |
| OTel 적용 서비스 | 4/5 (오류) | **2/5** (admin/clinical만) | `grep -l opentelemetry csBackend/*/src/main/resources/application*.properties` → 2개 매치. 2026-04-24 v2.1에서 정정 |

---

**문서 버전**: v2.2 (S-2 부분 완료 반영 — PR #2 `6ee695d`)
**생성일**: 2026-04-24
**v2.1 갱신**: 2026-04-24 (BFG 적용 직후 — S-1 완료 + OTel 4→2 정정 + SHA 매핑 갱신)
**v2.2 갱신**: 2026-04-24 (PR #2 머지 직후 — S-2 gateway 측 완료, S-2-B 후속 PR로 cross-service 이관)
**다음 갱신 기준**: 각 항목 완료 시 체크박스 + `docs:` 커밋
