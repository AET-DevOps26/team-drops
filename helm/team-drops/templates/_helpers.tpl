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
{{- end -}}

{{/*
These labels match the PVC templates created by chart 0.1.0. StatefulSet
volumeClaimTemplates are immutable, so do not derive them from chart metadata.
*/}}
{{- define "team-drops.legacyVolumeClaimLabels" -}}
helm.sh/chart: team-drops-0.1.0
app.kubernetes.io/name: team-drops
app.kubernetes.io/instance: team-drops
app.kubernetes.io/version: "0.1.0"
app.kubernetes.io/managed-by: Helm
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

{{- define "team-drops.keycloakPublicUrl" -}}
{{- if .Values.frontend.keycloakUrl -}}
{{- trimSuffix "/" .Values.frontend.keycloakUrl -}}
{{- else if and .Values.ingress.enabled .Values.ingress.host -}}
{{- printf "https://%s" .Values.ingress.host -}}
{{- else -}}
{{- "http://localhost:8090" -}}
{{- end -}}
{{- end -}}
