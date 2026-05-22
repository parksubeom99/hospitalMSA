#!/usr/bin/env bash
# ============================================================
# hospitalMSA — K8s Secret 생성 템플릿 (Phase 2)
# ============================================================
# Secret YAML은 git에 커밋하지 않는다 (base64는 암호화가 아님).
# 아래 명령을 실행해 클러스터에 직접 Secret을 생성한다.
# 실값은 placeholder다 — 로컬 검증 시 실제 값으로 바꿔 실행할 것.
# ============================================================

# --- oracle-secret : Oracle XE 자격증명 (Phase 2, Oracle Edition) ---
#   ORACLE_PWD      : Oracle XE SYS/SYSTEM 비밀번호 (DB 컨테이너용)
#   ORACLE_PASSWORD : 앱 서비스 스키마(his_*) 공통 비밀번호 (백엔드용)
kubectl create secret generic oracle-secret \
  --from-literal=ORACLE_PWD='<SYS_PASSWORD>' \
  --from-literal=ORACLE_PASSWORD='<SCHEMA_PASSWORD>' \
  --dry-run=client -o yaml | kubectl apply -f -

# --- app-secret : 애플리케이션 공유 시크릿 (Phase 3) ---
kubectl create secret generic app-secret \
  --from-literal=HIS_JWT_SECRET='<JWT_SECRET>' \
  --dry-run=client -o yaml | kubectl apply -f -
