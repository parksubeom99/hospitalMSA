# argocd-portforward.ps1
# ─────────────────────────────────────────────────────────────
# ArgoCD UI 접근용 kubectl port-forward 자동 재시작 래퍼.
#
# 왜 필요한가:
#   Docker Desktop K8s는 kind 기반(desktop-control-plane 컨테이너)이라
#   Service type NodePort/LoadBalancer가 호스트 localhost에 자동 노출되지 않음.
#   따라서 UI 접근은 kubectl port-forward로만 가능.
#   port-forward 프로세스는 파드 재시작이나 네트워크 끊김 시 종료되므로 자동 재시작 필요.
#
# 사용법:
#   수동 실행:
#     powershell -ExecutionPolicy Bypass -File scripts\argocd-portforward.ps1
#
#   부팅시 자동 실행 (Windows Task Scheduler):
#     1. 작업 스케줄러 → 작업 만들기
#     2. 트리거: 로그온할 때
#     3. 동작: powershell.exe
#        인수: -ExecutionPolicy Bypass -WindowStyle Hidden -File "C:\dev\potpolio\hospitalproject\scripts\argocd-portforward.ps1"
#
# 종료: Ctrl+C 또는 PowerShell 창 닫기
#
# 접속: https://localhost:8080  (admin / kubectl로 조회한 초기 비밀번호)
# ─────────────────────────────────────────────────────────────

$ErrorActionPreference = "Continue"
$LocalPort = 8080
$Service = "svc/argocd-server"
$Namespace = "argocd"
$TargetPort = 443

Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] ArgoCD port-forward auto-restart wrapper 시작" -ForegroundColor Cyan
Write-Host "  접속 URL: https://localhost:$LocalPort" -ForegroundColor Cyan
Write-Host "  중단: Ctrl+C" -ForegroundColor Yellow
Write-Host ""

while ($true) {
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] kubectl port-forward $Service -n $Namespace ${LocalPort}:${TargetPort}" -ForegroundColor Green
    kubectl port-forward $Service -n $Namespace "${LocalPort}:${TargetPort}"
    $exitCode = $LASTEXITCODE
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] port-forward 종료 (exit $exitCode). 5초 후 재시작..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
}
