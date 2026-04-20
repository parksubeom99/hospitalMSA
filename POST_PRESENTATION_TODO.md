# 발표 후 작업 리스트 (기록일: 2026-04-20)

발표일(2026-04-23) 이후 면접 준비 단계에서 순차 진행할 작업 목록.
각 항목은 독립적이므로 우선순위에 따라 개별 PR 또는 브랜치로 처리 권장.

---

## 1. Gateway OTel 계측 추가 (최우선 — 관측성 완결)

### 배경
- 현재 admin-master-service, clinical-service는 Phase 2-E에서 OpenTelemetry 설정 완료 → Grafana Tempo에 trace 정상 기록.
- **gateway-service는 설정 누락** → Tempo Service Name 드롭다운에 `gateway-service` 없음.
- 결과: HTTP end-to-end trace(브라우저 → Gateway → Admin)를 Waterfall 뷰로 보여줄 수 없음.

### 증상
- Grafana Explore → Tempo → Service Name 드롭다운에 `admin-master-service`, `clinical-service`만 존재 (gateway-service 없음)
- 이전 세션에서 캡처된 `새 폴더/3.webp`의 `http get /admin/dashboard/summary` 같은 HTTP 전체 체인 trace 생성 불가

### 해결 방법
`docker-compose-oracle.yml`의 `gateway-service` 섹션에 OTel Java agent + 환경변수 추가:
```yaml
gateway-service:
  environment:
    JAVA_TOOL_OPTIONS: "-javaagent:/otel/opentelemetry-javaagent.jar"
    OTEL_SERVICE_NAME: gateway-service
    OTEL_EXPORTER_OTLP_ENDPOINT: http://tempo:4317
    OTEL_TRACES_EXPORTER: otlp
    OTEL_METRICS_EXPORTER: none
    OTEL_LOGS_EXPORTER: none
    # ...기존 env...
  volumes:
    - ./docker/otel/opentelemetry-javaagent.jar:/otel/opentelemetry-javaagent.jar:ro
```

### 검증
- 재빌드 후 `docker logs psb-gateway`에서 `OpenTelemetry Javaagent` 초기화 로그 확인
- Grafana Tempo Service Name 드롭다운에 `gateway-service` 등장 확인
- 브라우저 API 호출 후 Waterfall 뷰에 Gateway → Admin 2-span chain 표시 확인

---

## 2. token.txt Git 이력 완전 삭제 (보안)

### 배경
- 2026-04-20 `chore(security)` 커밋(`8bd4e2d`)에서 `.gitignore` + `git rm --cached`로 추적만 제거.
- 과거 커밋 `386bdad`(2026-04-16)에 토큰 파일 이력 남아있음.
- JWT 토큰은 이미 만료되어 즉각적 위험은 없으나, 면접 시 보안 의식 평가에 부정적.

### 해결 방법
`git filter-repo` 사용하여 전체 이력에서 파일 제거:
```bash
pip install git-filter-repo  # 또는 choco install git-filter-repo
git filter-repo --path token.txt --invert-paths
git push origin --force --all
```

### 주의
- force push이므로 PR #1이 merge된 후 진행 권장 (이력 해시 전부 변경됨)
- 협업자 없는 단독 저장소라 안전

---

## 3. DatePicker 교체 (UX)

### 배경
- 현재 `ReceptionScreen.tsx`에서 브라우저 네이티브 `datetime-local` 사용.
- 회장님 지적: "선택완료 버튼 없음", "시·분 무한 스크롤" UX 문제.

### 해결 방법
```bash
cd csFrontend
npm install react-datepicker
```
- `ReceptionScreen.tsx`의 3곳 `<input type="datetime-local">` 교체
- 한글 locale 설정
- 유한 시·분 선택 UI 적용
- 선택완료 버튼 자동 제공됨

---

## 4. 임시 patient_id(Date.now) 구조 개선

### 배경
- 현재 `ReceptionScreen.tsx`에서 예약·접수 시 `tempPatientId = Date.now()` 사용.
- 결과: `patient` 테이블에 임시 환자(patient_id=1.7766E+12)가 계속 누적.
- 현재는 시드 리셋 후 누적되지만, 운영 환경에서는 데이터 쓰레기 → 구조적 결함.

### 해결 방법
서버에 통합 API 1개 추가:
```
POST /admin/patients/upsert-by-name-phone
  { name, phone, gender, rrnFront, rrnBack }
  → 기존 환자 있으면 반환, 없으면 신규 생성 + 반환
```
프론트는 이 API의 응답 `patient_id`를 사용.

---

## 5. CI Node 24 업그레이드 (인프라 유지보수)

### 배경
- 2025-09-19 GitHub Actions가 Node 24로 이전 예고.
- 강제 전환: **2026-06-02**
- Node 20 runner 완전 제거: 2026-09-16
- 현재 CI 실행마다 "Node.js 20 actions are deprecated" warning.

### 해결 방법 (3가지 중 택1)
**A. 임시 환경변수 (빠른 대응)**
```yaml
env:
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: "true"
```
**B. Node 24 지원 버전으로 업그레이드** (장기 대응)
- `actions/checkout@v4` → Node 24 지원 버전 확인 후 업그레이드
- `actions/setup-java@v4`, `actions/cache@v4`, `actions/upload-artifact@v4` 동일

**C. Self-hosted runner 사용**
- 규모 있는 프로젝트에서는 고려. 현재 개인 프로젝트엔 과함.

---

## 6. HospitalStore ↔ 서버 상태 계층 일원화

### 배경
- 현재 일부 화면은 `state.reservations`/`state.visits`(로컬) + React Query(서버) 병존.
- 이번 세션에서 로컬 시드를 빈 배열로 만들고 fallback 조건 완화했지만, 완전 서버 우선으로 일원화되지는 않음.

### 해결 방법
- React Query 쿼리를 모든 화면에서 필수 사용
- 로컬 HospitalStore는 **세션/UI 상태 전용**으로 용도 축소 (patients/reservations/visits/soaps 등 서버 데이터 필드 제거)
- 시연 모드(데모초기화) 지원이 필요하면 "읽기 전용 로컬 샘플" 별도 슬라이스로 분리

---

## 7. 테스트 커버리지 확장 (면접 약점 방어)

### 배경
- 현재: 2 파일 / 7 케이스 (`VisitServiceTest` 4 + `VisitRegisteredConsumerTest` 3)
- 실제 Controller 47개 대비 비중 낮음 → 면접관이 "Test 부족" 찌를 수 있음
- 발표 슬라이드에 "1개 Test"로 자인한 수치도 **실제 7 케이스로 상향 정정** 필요

### 해결 방법
- 우선순위 높은 3개 Consumer 테스트 추가: `BillingRequestedConsumer`, `BillingCompletedConsumer`, `DiagnosticOrderCompletedConsumer`
- `ReservationService.checkIn` 시나리오 테스트 (BOOKED → CHECKED_IN + Visit 자동 생성)
- 목표: 최소 **15 케이스 / 5 파일**로 확장
- 면접 답변 라인: "핵심 Saga 체인 Consumer 집중 테스팅 → 전체 경로 중 고위험 구간 우선 커버"

---

## 8. Saga 명시적 클래스화 (면접 대응 강화)

### 배경
- 현재: Choreography 방식이라 `*Saga*.java` 파일 0건
- 면접관이 "Saga 오케스트레이터 어디 있나요?" 질문 시 코드 네비게이션 증거 부족
- Choreography를 정확히 이해하는 면접관이라면 문제없지만, 시각적 증거가 있으면 안전

### 해결 방법
- `kr.co.seoulit.his.{admin|clinical}.saga.VisitBillingSagaFlow.java` 같은 **문서화용 플로우 클래스** 추가
- 실제 로직이 아니라 **상수·주석·JavaDoc으로 이벤트 흐름 집약**:
```java
/**
 * Visit → Billing Saga (Choreography)
 *
 * 1. Admin.VisitService.create → VISIT_REGISTERED 발행
 * 2. Clinical.VisitRegisteredConsumer → visit_clinical_status WAITING
 * 3. Clinical.SoapService.save → EXAM_REQUESTED
 * ...
 */
public final class VisitBillingSagaFlow {
    public static final String EVT_VISIT_REGISTERED = "VISIT_REGISTERED";
    ...
}
```
- **Choreography 원칙 유지**하면서 탐색성·설명 가능성 향상

---

## 9. K8s / Helm 도입 (Ops 계층)

### 배경
- 현재: Docker Compose 12 컨테이너
- MSA의 "독립 배포" 증명에는 충분하나, 실무 면접관은 K8s 경험 여부 확인 경향
- Phase 3 AWS 이전과 연계하면 자연스러움

### 해결 방법 (학습 + 이전 2 트랙)
- **학습 트랙:** Deployment / Service / Ingress / ConfigMap / Secret 기본 리소스 작성
- **이전 트랙:**
  - Step A: AWS ECS(Fargate) — 컨테이너 오케스트레이션 첫 경험
  - Step B: AWS EKS(Kubernetes) — 풀 K8s
  - Step C: Helm 차트로 재사용 가능한 배포 단위 정의

### 면접 답변 라인
- "MSA 본질(서비스 분리·이벤트 통신)은 Docker Compose에서 검증 완료 → K8s는 Ops 레이어라 우선순위상 후순위였음. Phase 3에서 EKS + Helm으로 확장 예정."

---

## 10. 자동 Compensation 트랜잭션 (Saga 완성)

### 배경
- 현재: `VisitClinicalStatusService.markBillingFailed`는 visit을 `BILLING_FAILED` 상태로 전환 + 담당자 수동 재처리 대기
- 완전 자동 Compensating Transaction 아님
- 면접에서 "결제 실패 시 자동 롤백은?" 질문 대응 필요

### 해결 방법
- 공통 `SagaCompensationHandler` 추상화: 각 이벤트별 보상 핸들러 등록 방식
- 예: BILLING_FAILED 수신 시 조건부 자동 복구
  - visit.status가 COMPLETED 였으면 → BILLABLE 복귀 (재청구 가능 상태)
  - invoice가 PAID 이전이면 → 단순 상태만 롤백
- **주의**: 의료업무 특성상 "완전 자동 롤백"은 현업과 안 맞을 수 있음. **조건부 자동 + 담당자 확인** 하이브리드 권장

### 면접 답변 라인 (완성 후)
- "BILLING_FAILED 수신 시 SagaCompensationHandler가 조건 평가 후 자동/수동 경로 분기. 완전 자동 대신 **도메인 맥락 반영한 하이브리드 보상**."

---

## 11. 프론트엔드 커버리지 확장

### 배경
- 현재: Controller 47개 vs 프론트 주요 화면 8개 (접수/진료/오더/수납/대시보드/마스터/로그인/감사로그)
- 백엔드 대비 프론트 활용 비중 낮음

### 해결 방법 (우선순위 순)
1. **환자 상세 조회 화면** — `/admin/patients/{id}` API 기반
2. **감사로그 필터·검색** — 현재 텍스트 리스트만, 기간/사용자/이벤트유형 필터 추가
3. **청구 상세 화면** — 청구 항목별 breakdown, 조정(adjustment) 내역 표시
4. **예약 캘린더 뷰** — 월간/주간 캘린더에 예약 마킹 (react-big-calendar 등)
5. **Admin 대시보드 위젯 추가** — 시간대별 방문 그래프, 의사별 방문 건수

### 면접 답변 라인
- "프론트는 **핵심 업무 흐름(접수→진료→수납) 완결성** 우선, 부가 화면은 선택적 확장. 실제 운영에서 사용 빈도 높은 화면부터 구축."

---

## 참고: 이번 세션에서 해결된 주요 이슈 (2026-04-18~20)

- [x] Kafka VISIT_REGISTERED → visit_clinical_status `patient_name` 영속화 (V3 migration)
- [x] Gateway WebFlux Security + CORS 정상화 (401 루프 해결)
- [x] 시드 SQL 축소 + 번호 체계(예약 21xxx / 접수 11xxx) 분리
- [x] 프론트 로컬 시드 fallback 제거 (유령 데이터 차단)
- [x] 예약 조회 전체기간 + 자동 채움 + 체크인 분기 + 수정 폼 populate + 성별 F/M
- [x] VisitResponse patient JOIN (gender/rrnMasked/patientPhone)
- [x] Dashboard 예약 집계 admin_appointment → admin_reservation 교정
- [x] CI 실패 테스트 복구 (VisitServiceTest Mock 추가, VisitRegisteredConsumerTest 2인자 시그니처 반영)
- [x] PR #1 merge 가능 상태 확보 (CI Green + 4 successful checks)
