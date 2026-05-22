{{/*
공통 라벨 — selector 라벨(app.kubernetes.io/name)은 각 워크로드가 직접 지정하고,
이 헬퍼는 selector에 들어가지 않는 부가 라벨만 제공한다 (Helm 업그레이드 시 selector 불변 보장).
*/}}
{{- define "hospitalmsa.commonLabels" -}}
app.kubernetes.io/part-of: hospitalmsa
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}
