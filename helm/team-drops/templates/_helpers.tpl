{{- define "team-drops.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "team-drops.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "team-drops.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/name: {{ include "team-drops.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: team-drops
monitoring: "true"
{{- end -}}

{{- define "team-drops.selectorLabels" -}}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: team-drops
app.kubernetes.io/component: {{ .component }}
{{- end -}}

{{- define "team-drops.image" -}}
{{ .root.Values.image.registry }}/{{ .root.Values.image.owner }}/{{ .image }}:{{ .root.Values.image.tag }}
{{- end -}}

{{- define "team-drops.imagePullSecrets" -}}
{{- if .Values.imagePullSecrets }}
imagePullSecrets:
{{- range .Values.imagePullSecrets }}
  - name: {{ . | quote }}
{{- end }}
{{- end }}
{{- end -}}

{{- define "team-drops.requiredIngressHost" -}}
{{- required "ingress.host is required when ingress.enabled=true" .Values.ingress.host -}}
{{- end -}}
