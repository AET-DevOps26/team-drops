# Team Drops Helm Deployment and Operations

Team Drops is deployed as two Helm releases:

| Release | Chart | Namespace | Contents |
| --- | --- | --- | --- |
| `team-drops` | `helm/team-drops` | `team-drops` | Frontend, APIs, Keycloak, PostgreSQL, and MongoDB |
| `team-drops-monitoring` | `helm/team-drops-monitoring` | `team-drops` | Prometheus, Grafana, Alertmanager, Loki, Alloy, and kube-state-metrics |

The `team-drops` namespace must already exist. The GitHub Actions deployment
deliberately does not create, label, or otherwise modify Namespace objects.

## Prerequisites

- Helm 4.2.0 and `kubectl` configured for the Rancher cluster
- Permission to manage namespaced workloads, Secrets, Roles, RoleBindings, and
  both Helm releases in `team-drops`
- A default dynamic storage class supporting `ReadWriteOnce` volumes
- The `team-drops` namespace created once through Rancher by an authorized user

The Rancher monitoring profile requires no privileged Pod Security policy. It
does not deploy node-exporter, DaemonSets, host networking, host PID, hostPath
mounts, ClusterRoles, or ClusterRoleBindings.

kube-state-metrics uses a Role in `team-drops` limited to `list` and `watch` on
pods and deployments. This is sufficient for readiness, replica, and restart
metrics and avoids the cluster-resource permissions rejected by Rancher.

## Automatic deployment

`.github/workflows/deploy-kubernetes.yml` runs after a successful main-branch
Docker publication and can also be started manually. It validates both charts,
performs a server-side dry-run of a small non-privileged pod, creates or updates
namespaced credential Secrets, upgrades the application to retire legacy
monitoring resources, and then deploys and verifies the monitoring release.

The workflow pins Helm 4.2.0 and uses its `legacy` polling wait strategy. This
avoids the watcher strategy's repeated HTTP/2 stream failures through the
Rancher API proxy. The preflight Pod is never persisted. It makes namespace
quota, LimitRange, Pod Security, and other admission failures visible before a
15-minute Helm wait.

Required GitHub repository secrets:

| Secret | Purpose |
| --- | --- |
| `KUBE_CONFIG` | Kubeconfig for a Rancher identity with access to `team-drops` |
| `LLM_API_KEY` | API key used by the Rancher OpenAI-compatible GenAI configuration |
| `GRAFANA_ADMIN_PASSWORD` | Password stored in `team-drops-monitoring-grafana-admin` |
| `ALERTMANAGER_SLACK_WEBHOOK_URL` | Slack incoming webhook for `#team-drops-alerts` |

Optional secrets are needed only when their Alertmanager receiver is enabled:

- `ALERTMANAGER_SMTP_PASSWORD`
- `ALERTMANAGER_PAGERDUTY_ROUTING_KEY`

Alertmanager credentials are stored in
`team-drops-monitoring-alertmanager-credentials` in `team-drops` and mounted as
files. Grafana credentials are stored in
`team-drops-monitoring-grafana-admin` in the same namespace.

Automatic deployments use the immutable `sha-<commit>` image tag published by
the Docker workflow. Manual workflow runs default to `latest` but accept an
explicit image tag.

## Render and validate

Build the monitoring dependencies first:

```bash
helm dependency build ./helm/team-drops-monitoring
```

Lint both releases. The dummy API key is used only for local rendering:

```bash
helm lint ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set genai.llmApiKey=dummy

helm lint ./helm/team-drops-monitoring \
  --namespace team-drops \
  -f helm/team-drops-monitoring/values-rancher.yaml
```

Render manifests without changing the cluster:

```bash
helm template team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set genai.llmApiKey=dummy \
  > /tmp/team-drops-app-rendered.yaml

helm template team-drops-monitoring ./helm/team-drops-monitoring \
  --namespace team-drops \
  -f helm/team-drops-monitoring/values-rancher.yaml \
  > /tmp/team-drops-monitoring-rendered.yaml
```

Expected results:

- Application images point to `ghcr.io/aet-devops26/...`.
- Application and monitoring workloads render in `team-drops`, while their
  separate Helm releases retain distinct ownership and resource names.
- Monitoring Roles and RoleBindings grant namespaced read-only access to
  application metrics, Kubernetes state, and application pod logs.
- The kube-state-metrics Role contains only `list` and `watch` for pods and
  deployments in `team-drops`.
- No cluster-scoped RBAC or privileged host access is rendered.
- Backend environment variables never contain both `value` and `valueFrom`.

When the current identity supports server-side dry runs:

```bash
kubectl apply --dry-run=server -n team-drops \
  -f /tmp/team-drops-app-rendered.yaml
kubectl apply --dry-run=server -n team-drops \
  -f /tmp/team-drops-monitoring-rendered.yaml
```

## Manual installation

Create the monitoring Secrets before the first installation. Use local files
for alerting credentials to avoid putting their contents in shell history:

```bash
kubectl -n team-drops create secret generic team-drops-monitoring-alertmanager-credentials \
  --from-file=slack-webhook-url=/secure/path/slack-webhook-url \
  --from-file=smtp-password=/secure/path/smtp-password \
  --from-file=pagerduty-routing-key=/secure/path/pagerduty-routing-key

kubectl -n team-drops create secret generic team-drops-monitoring-grafana-admin \
  --from-literal=admin-user=admin \
  --from-literal=admin-password=<password>
```

Only the Slack credential is required by `values-rancher.yaml`; omit the SMTP
and PagerDuty files while those receivers are disabled.

Upgrade the application first, then install monitoring:

```bash
helm dependency build ./helm/team-drops-monitoring

helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set image.tag=sha-<commit> \
  --set genai.llmApiKey=<api-key> \
  --wait=legacy --timeout 10m

helm upgrade --install team-drops-monitoring ./helm/team-drops-monitoring \
  --namespace team-drops \
  -f helm/team-drops-monitoring/values-rancher.yaml \
  --wait=legacy --timeout 15m
```

During the first shared-namespace cutover, the monitoring release creates fresh
PVCs and uses the `team-drops-monitoring-` resource prefix. The application
upgrade runs first so Helm removes obsolete bundled Prometheus and Grafana
resources through their original ownership and frees shared namespace quota.
Do not manually relabel or adopt old resources into the monitoring release.

The default monitoring profile is sized for the shared Rancher namespace. Its
six workloads have approximately 576 MiB of aggregate memory requests and
1.4 GiB of aggregate memory limits. Prometheus and Loki each retain a 384 MiB
limit; lowering them further can cause out-of-memory restarts during ingestion.
These settings reduce quota pressure but do not change the Rancher-managed
namespace quota.

If GHCR images are private, create an image-pull Secret in `team-drops` and add
`--set imagePullSecrets[0]=<secret-name>` to the application installation.

## Verify the application

```bash
kubectl -n team-drops get pods,svc,ingress,pvc
kubectl -n team-drops rollout status deployment/frontend
kubectl -n team-drops rollout status deployment/user-service
kubectl -n team-drops rollout status deployment/learning-service
kubectl -n team-drops rollout status deployment/progress-feedback-service
kubectl -n team-drops rollout status deployment/genai-service
kubectl -n team-drops rollout status statefulset/postgres
kubectl -n team-drops rollout status statefulset/mongo
```

Check internal health endpoints without exposing them through Ingress:

```bash
kubectl -n team-drops port-forward svc/user-service 8081:80
curl http://localhost:8081/actuator/health
```

```bash
kubectl -n team-drops port-forward svc/genai-service 8084:80
curl http://localhost:8084/health
```

For failures, inspect the pod, logs, and namespace events:

```bash
kubectl -n team-drops describe pod <pod-name>
kubectl -n team-drops logs <pod-name>
kubectl -n team-drops get events --sort-by=.lastTimestamp
```

The `monitoring: "true"` and original chart-version labels on the PostgreSQL and
MongoDB `volumeClaimTemplates` are retained for compatibility with existing
StatefulSets. That template section is immutable; changing it requires a
controlled StatefulSet recreation while preserving the PVCs.

## Monitoring stack

The Rancher profile provisions persistent internal-only services:

| Component | Retention/storage | Purpose |
| --- | --- | --- |
| Prometheus | 7 days, 2 GiB PVC | Application and Kubernetes-state metrics |
| Grafana | 1 GiB PVC | Provisioned dashboards and Prometheus/Loki datasources |
| Loki | 3 days, 2 GiB PVC | Logs from `team-drops` only |
| Alertmanager | 1 GiB PVC | Grouping, silencing, and notification routing |
| Alloy | Ephemeral | Kubernetes API log collection from `team-drops` |
| kube-state-metrics | Ephemeral | Pod and deployment state from `team-drops` only |

The monitoring chart does not create ResourceQuota or LimitRange objects. These
policies affect every workload in the shared namespace and remain under Rancher
administration. Existing project quota must have enough remaining capacity for
the persistent monitoring stack. Monitoring services have no public Ingress.

Verify the monitoring workloads and namespaced access:

```bash
kubectl -n team-drops get pods,svc,pvc
kubectl -n team-drops get deploy,statefulset,replicaset -o wide
kubectl -n team-drops get resourcequota,limitrange -o yaml
kubectl -n team-drops get events --sort-by=.lastTimestamp
kubectl -n team-drops get role,rolebinding
```

### Grafana dashboards

```bash
kubectl -n team-drops port-forward svc/team-drops-monitoring-grafana 3000:80
```

Open <http://localhost:3000> and sign in as `admin` using
`GRAFANA_ADMIN_PASSWORD`. The **Team Drops** folder contains:

- **Team Drops Overview**: 13 application traffic, reliability, deployment,
  and runtime panels
- **Team Drops Logs**: log volume, error volume, recent logs, and error streams

### Prometheus metrics

```bash
kubectl -n team-drops port-forward \
  svc/team-drops-monitoring-prometheus 9090:80
```

Open <http://localhost:9090>. Useful queries include:

```promql
up{job="team-drops-services"}
application_info{namespace="team-drops"}
sum by (service) (rate(http_server_requests_seconds_count{namespace="team-drops"}[5m]))
increase(kube_pod_container_status_restarts_total{namespace="team-drops"}[15m])
ALERTS{alertname=~"TeamDrops.+"}
```

The `up` query should show four healthy API targets. The `application_info`
version label should match the deployed `sha-<commit>` image tag.

If Rancher reports that the deployment user is attempting to grant RBAC
permissions it does not hold, inspect the rendered
`team-drops-monitoring-state-metrics` Role. It must contain only pods and
deployments. The chart deliberately sets an explicit collector allowlist so
kube-state-metrics defaults cannot add cluster resources such as admission
webhooks, certificate requests, storage classes, or volume attachments.

A failed monitoring revision can be recovered by fixing the values and running
the same `helm upgrade --install` command again. Helm reconciles its uniquely
named resources without taking ownership of application resources.

If every monitoring workload shows one desired replica but zero current pods,
the workload controllers are being prevented from creating Pods. Ask a Rancher
administrator to inspect project/namespace quota, LimitRanges, Pod Security,
and admission policy. Do not delete bound monitoring PVCs while correcting the
namespace. The automatic deployment prints these policies, workload
descriptions, and `team-drops` events whenever a run fails.

### Loki logs

In Grafana Explore, choose the `Loki` datasource and use LogQL such as:

```logql
{namespace="team-drops"}
{namespace="team-drops", component="genai-service"}
{namespace="team-drops"} |~ "(?i)(error|exception|failed)"
```

Alloy discovers new and restarted application pods automatically. It attaches
only bounded namespace, pod, container, application, and component labels. To
inspect collection directly:

```bash
kubectl -n team-drops logs deployment/team-drops-monitoring-alloy
kubectl -n team-drops port-forward svc/team-drops-monitoring-loki 3100:3100
curl http://localhost:3100/ready
curl http://localhost:3100/loki/api/v1/labels
```

## Alerting

Four application alerts are installed:

- `TeamDropsServiceDown`: a service cannot be scraped for two minutes
- `TeamDropsHighErrorRate`: more than 5% HTTP 5xx responses for five minutes
  while receiving traffic
- `TeamDropsSlowResponses`: mean latency above 1.5 seconds for five minutes
  while receiving traffic
- `TeamDropsPodRestartBurst`: more than five container restarts in 15 minutes

Warnings route to enabled Slack and email receivers. Critical alerts route to
enabled Slack, email, and PagerDuty receivers. Every receiver sends resolved
notifications, while unrelated alerts use a null receiver.

Access Alertmanager to inspect alerts and create silences:

```bash
kubectl -n team-drops port-forward svc/team-drops-monitoring-alertmanager 9093:9093
```

Open <http://localhost:9093>. Slack is enabled in
`helm/team-drops-monitoring/values-rancher.yaml`. To enable email, configure the
non-secret SMTP fields and set `monitoring.alertmanager.email.enabled=true`. To
enable PagerDuty, set `monitoring.alertmanager.pagerduty.enabled=true`. Store
their credentials in the repository secrets listed above.

After manually changing receiver configuration or credentials:

```bash
kubectl -n team-drops rollout restart statefulset/team-drops-monitoring-alertmanager
kubectl -n team-drops rollout status statefulset/team-drops-monitoring-alertmanager
```

The application chart also supports Operator-compatible `ServiceMonitor` and
`PrometheusRule` resources. They are disabled in the Rancher values because the
separately managed standalone Prometheus release is used.

## GenAI options

The Rancher profile uses an OpenAI-compatible endpoint and requires
`genai.llmApiKey`. Always deploy a specific immutable image tag.

For an infrastructure-only test without an API key, override the provider to
Ollama. AI requests will fail unless an Ollama backend is reachable:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set image.tag=sha-<commit> \
  --set genai.llmProvider=ollama
```

To opt into the chart's in-cluster Ollama workload, also set
`ollama.enabled=true`.

## TLS and browser access

The Rancher values configure the project ingress host and TLS. Browsers may
enforce HTTPS for `*.stud.k8s.aet.cit.tum.de` through HSTS. The cert-manager
issuer must be supplied by the cluster administrators; do not guess an issuer
name.

To override it during a manual installation:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set ingress.tls.enabled=true \
  --set ingress.tls.clusterIssuer=<issuer-name>
```

## Rollback and removal

```bash
helm history team-drops -n team-drops
helm rollback team-drops <revision> -n team-drops
helm history team-drops-monitoring -n team-drops
helm rollback team-drops-monitoring <revision> -n team-drops
```

Uninstalling releases does not automatically remove every PVC or manually
created Secret. Review retained data before deleting it:

```bash
helm uninstall team-drops -n team-drops
helm uninstall team-drops-monitoring -n team-drops
kubectl -n team-drops get pvc,secret
```

### Clean up the failed legacy namespace release

The failed `team-drops-monitoring` release and its PVCs in `drops-monitoring`
are not modified during cutover. After the new release in `team-drops` is
healthy and its dashboards, metrics, logs, and alerts are verified, inspect the
old resources:

```bash
helm status team-drops-monitoring -n drops-monitoring
kubectl -n drops-monitoring get pods,svc,pvc,secret
kubectl -n team-drops get role,rolebinding \
  -l app.kubernetes.io/instance=team-drops-monitoring
```

Removing that release or its PVCs permanently deletes retained monitoring data
and must be performed as a separate, explicitly approved cleanup operation.
The failed release may also own legacy read-only Roles in `team-drops`. The new
release uses distinct RBAC names so removing those legacy objects cannot break
the replacement stack.
