# Team Drops Helm Chart

Deploys the Team Drops frontend, backend services, GenAI service, Postgres, and
MongoDB into an existing Rancher namespace.

The frontend image builds the Vite app into static files and serves them with
Nginx on port 80.

## Render and validate safely

```bash
helm dependency build ./helm/team-drops-monitoring

helm lint ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set genai.llmApiKey=dummy

helm lint ./helm/team-drops-monitoring \
  --namespace drops-monitoring \
  -f helm/team-drops-monitoring/values-rancher.yaml

helm template team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set genai.llmApiKey=dummy \
  > /tmp/team-drops-app-rendered.yaml

helm template team-drops-monitoring ./helm/team-drops-monitoring \
  --namespace drops-monitoring \
  -f helm/team-drops-monitoring/values-rancher.yaml \
  > /tmp/team-drops-monitoring-rendered.yaml
```

Inspect the rendered manifests:

```bash
grep -n "image:" /tmp/team-drops-app-rendered.yaml
grep -n "host:" /tmp/team-drops-app-rendered.yaml
grep -n "storage:" /tmp/team-drops-monitoring-rendered.yaml
grep -n "namespace:" /tmp/team-drops-monitoring-rendered.yaml
```

Expected:

- app images point to `ghcr.io/aet-devops26/...`
- ingress hosts use `*.stud.k8s.aet.cit.tum.de`
- Postgres and Mongo storage defaults are small
- all monitoring workloads explicitly render into `drops-monitoring`
- read-only monitoring Roles and RoleBindings explicitly render into `team-drops`
- backend env vars use either `value` or `valueFrom`, never both

Validate against the Kubernetes API without creating resources:

```bash
kubectl apply --dry-run=server -n team-drops -f /tmp/team-drops-app-rendered.yaml
kubectl apply --dry-run=server -n drops-monitoring -f /tmp/team-drops-monitoring-rendered.yaml
```

## Install or update

Use the real ingress host for the namespace. For a branch-image test before
merging to `main`, use `--set image.tag=kubernetes`. OpenAI mode requires
`genai.llmApiKey`; infrastructure-only tests can use `genai.llmProvider=ollama`.
The committed `values-rancher.yaml` file contains the non-secret Rancher
settings: host, staging TLS, and Ollama mode.

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set image.tag=kubernetes
```

If GHCR packages are private, create an image pull secret in the namespace and
install with:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set imagePullSecrets[0]=<secret-name>
```

## Automatic deployment

The `Deploy Kubernetes` GitHub Actions workflow deploys this chart after the
`Docker Publish` workflow succeeds on `main`. It can also be started manually
from the Actions tab.

Required repository secret:

- `KUBE_CONFIG`: kubeconfig content for a Rancher user or service account with
  permission to manage workloads in the existing `team-drops` and
  `drops-monitoring` namespaces and manage both Helm releases.
- `GRAFANA_ADMIN_PASSWORD`: administrator password for Grafana; it is written
  only to `team-drops-grafana-admin` in the `drops-monitoring` namespace.
- `ALERTMANAGER_SLACK_WEBHOOK_URL`: incoming webhook used by Alertmanager for
  the `#team-drops-alerts` channel.

Optional repository secrets, required only when their receiver is enabled:

- `ALERTMANAGER_SMTP_PASSWORD`: SMTP password for email notifications.
- `ALERTMANAGER_PAGERDUTY_ROUTING_KEY`: PagerDuty Events API v2 integration
  key.

The workflow writes alerting credentials to the
`team-drops-alertmanager-credentials` Kubernetes Secret in `drops-monitoring`. They
are mounted as files and are never stored in Helm values or the Alertmanager
ConfigMap.
For a manual deployment, create the same Secret from local files before running
Helm:

```bash
kubectl -n drops-monitoring create secret generic team-drops-alertmanager-credentials \
  --from-file=slack-webhook-url=/secure/path/slack-webhook-url \
  --from-file=smtp-password=/secure/path/smtp-password \
  --from-file=pagerduty-routing-key=/secure/path/pagerduty-routing-key
```

Only the Slack file is required by `values-rancher.yaml`; omit files for
disabled receivers.

Automatic runs deploy the immutable `sha-<commit>` image tag published by the
`Docker Publish` workflow. Manual runs default to `latest`, but allow choosing a
specific image tag from the Actions tab. The workflow waits up to 15 minutes
for the monitoring stack and 10 minutes for the application stack.

## Check the deployment

```bash
kubectl -n team-drops get pods
kubectl -n team-drops get svc
kubectl -n team-drops get ingress
kubectl -n team-drops get pvc
```

Check app rollouts:

```bash
kubectl -n team-drops rollout status deployment/frontend
kubectl -n team-drops rollout status deployment/user-service
kubectl -n team-drops rollout status deployment/learning-service
kubectl -n team-drops rollout status deployment/progress-feedback-service
kubectl -n team-drops rollout status deployment/genai-service
kubectl -n team-drops rollout status statefulset/postgres
kubectl -n team-drops rollout status statefulset/mongo
```

Useful debugging commands:

```bash
kubectl -n team-drops describe pod <pod-name>
kubectl -n team-drops logs <pod-name>
kubectl -n team-drops get events --sort-by=.lastTimestamp
```

The `monitoring: "true"` labels on the Mongo and Postgres
`volumeClaimTemplates` are retained for upgrade compatibility with existing
StatefulSets. Their chart and application version labels also remain pinned to
the original `0.1.0` values. Kubernetes treats this section as immutable;
removing or changing these labels requires a controlled StatefulSet recreation
while preserving the existing PVCs.

Internal health checks via port-forward:

```bash
kubectl -n team-drops port-forward svc/user-service 8081:80
curl http://localhost:8081/actuator/health
```

```bash
kubectl -n team-drops port-forward svc/genai-service 8084:80
curl http://localhost:8084/health
```

## Prometheus, Loki, and Grafana monitoring

The observability stack is a separate Helm release in the dedicated
`drops-monitoring` namespace. The application remains in `team-drops`.
Prometheus, kube-state-metrics, and Alloy use read-only Roles and RoleBindings;
no cluster-scoped RBAC is created. Alloy can read pod logs only from
`team-drops`. A ResourceQuota and LimitRange bound monitoring CPU, memory,
storage, pod, PVC, and Service consumption independently from the application.
The stack does not deploy node-exporter, DaemonSets, host mounts, or other
privileged node-level collectors.

Prometheus retains seven days of data on a 2 GiB PVC. Loki retains three days
of logs on a separate 2 GiB PVC. Alertmanager and Grafana use 1 GiB PVCs. None
of the monitoring services is exposed through Ingress. The
`drops-monitoring` namespace must be created once in Rancher by an authorized
user; CI deliberately does not create, label, or otherwise modify the
Namespace object. No privileged Pod Security setting is required. For a manual
installation, create the namespaced secrets and install the second chart:

```bash
kubectl -n drops-monitoring create secret generic team-drops-alertmanager-credentials \
  --from-file=slack-webhook-url=/secure/path/slack-webhook-url
kubectl -n drops-monitoring create secret generic team-drops-grafana-admin \
  --from-literal=admin-user=admin \
  --from-literal=admin-password=<password>

helm dependency build ./helm/team-drops-monitoring
helm upgrade --install team-drops-monitoring ./helm/team-drops-monitoring \
  --namespace drops-monitoring \
  -f helm/team-drops-monitoring/values-rancher.yaml \
  --wait --timeout 15m
```

The first deployment of this split layout starts fresh monitoring PVCs in the
new namespace; Kubernetes volumes cannot be moved between namespaces. Existing
application database PVCs in `team-drops` are unaffected. After verifying the
new stack, any old manually-created alerting credential Secret in `team-drops`
can be removed.

Check the stack and persistent storage:

```bash
kubectl -n drops-monitoring get pods,svc,pvc,resourcequota,limitrange
kubectl -n team-drops get role,rolebinding | grep -E 'prometheus|kube-state|alloy'
```

Access Grafana:

```bash
kubectl -n drops-monitoring port-forward svc/team-drops-grafana 3000:80
```

Open `http://localhost:3000`, sign in as `admin` with the value of
`GRAFANA_ADMIN_PASSWORD`, and open **Dashboards > Team Drops > Team Drops
Overview**. The same folder also contains **Team Drops Logs**, which can filter
by component, pod, and container.

Access the Prometheus query and target UI:

```bash
kubectl -n drops-monitoring port-forward svc/team-drops-prometheus-server 9090:80
```

Open `http://localhost:9090`. Useful queries are:

```promql
up{job="team-drops-services"}
application_info{namespace="team-drops"}
sum(rate(http_server_requests_seconds_count{namespace="team-drops"}[5m]))
sum(rate(http_requests_total{namespace="team-drops"}[5m]))
increase(kube_pod_container_status_restarts_total{namespace="team-drops"}[15m])
```

Inspect raw application metrics without exposing them through Ingress:

```bash
kubectl -n team-drops port-forward svc/user-service 8081:80
curl http://localhost:8081/actuator/prometheus
```

```bash
kubectl -n team-drops port-forward svc/genai-service 8084:80
curl http://localhost:8084/metrics
```

`up{job="team-drops-services"}` should contain four healthy targets. The
`application_info` version label should match the deployed `sha-<commit>` image
tag. The dashboard separates request rate, HTTP 5xx percentage, and mean
latency by service, and also shows target availability and the number of firing
runtime alerts.

Access Loki directly for health and label checks:

```bash
kubectl -n drops-monitoring port-forward svc/team-drops-loki 3100:3100
curl http://localhost:3100/ready
curl http://localhost:3100/loki/api/v1/labels
```

In Grafana, open **Explore**, choose the `Loki` datasource, and use LogQL such
as:

```logql
{namespace="team-drops"}
{namespace="team-drops", component="genai-service"}
{namespace="team-drops"} |~ "(?i)(error|exception|failed)"
```

Alloy discovers new and restarted pods automatically through the Kubernetes
API. It attaches only the namespace, pod, container, application, and component
labels; it does not extract request IDs or other unbounded values. Confirm the
scope and delivery with:

```bash
kubectl -n drops-monitoring logs deployment/team-drops-alloy
kubectl -n team-drops logs deployment/genai-service --tail=5
curl -G http://localhost:3100/loki/api/v1/query_range \
  --data-urlencode 'query={namespace="team-drops"}' \
  --data-urlencode 'limit=20'
```

Four alert rules are installed with the standalone Prometheus stack:

- `TeamDropsServiceDown` fires after a service cannot be scraped for 2 minutes.
- `TeamDropsHighErrorRate` fires when more than 5% of requests fail with HTTP
  5xx for 5 minutes, provided the service is receiving traffic.
- `TeamDropsSlowResponses` fires when mean latency is above 1.5 seconds for 5
  minutes, provided the service is receiving traffic.
- `TeamDropsPodRestartBurst` fires when a container restarts more than 5 times
  in 15 minutes.

Open **Alerts** in Prometheus or use this query to inspect active alerts:

```promql
ALERTS{alertname=~"TeamDrops.+"}
```

Access Alertmanager to inspect routed alerts and create temporary silences:

```bash
kubectl -n drops-monitoring port-forward svc/team-drops-alertmanager 9093:9093
```

Open `http://localhost:9093`. Warning alerts are sent to enabled Slack and
email receivers. Critical alerts are sent to enabled Slack, email, and
PagerDuty receivers. Resolved notifications are enabled for every receiver;
unrelated alerts go to a null receiver.

With the port-forward running, send a five-minute synthetic warning to verify
Slack routing and the resolved notification:

```bash
STARTS_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)
ENDS_AT=$(date -u -d '+5 minutes' +%Y-%m-%dT%H:%M:%SZ)
curl -X POST http://localhost:9093/api/v2/alerts \
  -H 'Content-Type: application/json' \
  -d "[{\"labels\":{\"alertname\":\"TeamDropsRoutingTest\",\"team\":\"team-drops\",\"severity\":\"warning\",\"service\":\"manual-test\"},\"annotations\":{\"summary\":\"Team Drops routing test\"},\"startsAt\":\"$STARTS_AT\",\"endsAt\":\"$ENDS_AT\"}]"
```

Slack is enabled by `helm/team-drops-monitoring/values-rancher.yaml`. To enable email, set
`monitoring.alertmanager.email.enabled=true` and provide `smarthost`, `from`,
`to`, and optionally `authUsername`. To enable PagerDuty, set
`monitoring.alertmanager.pagerduty.enabled=true`. Store the corresponding
password or routing key in the GitHub repository secrets listed above rather
than passing it through Helm. After changing receiver values or manually
updating the credential Secret, reload Alertmanager with:

```bash
kubectl -n drops-monitoring rollout restart statefulset/team-drops-alertmanager
kubectl -n drops-monitoring rollout status statefulset/team-drops-alertmanager
```

The automatic deployment workflow performs this reload after every Helm
upgrade.

When the chart uses Rancher Monitoring instead, the same rules are installed as
a `PrometheusRule`. Set `monitoring.alerts.enabled=false` to omit that resource.
Rancher `ServiceMonitor` resources remain available through
`monitoring.rancherServiceMonitors.enabled`, but are disabled in
`values-rancher.yaml` because this stack does not use the cluster Prometheus.

Rollback or uninstall:

```bash
helm history team-drops -n team-drops
helm rollback team-drops <revision> -n team-drops
helm history team-drops-monitoring -n drops-monitoring
helm rollback team-drops-monitoring <revision> -n drops-monitoring
helm uninstall team-drops -n team-drops
helm uninstall team-drops-monitoring -n drops-monitoring
kubectl -n drops-monitoring delete secret team-drops-alertmanager-credentials team-drops-grafana-admin
```

## GenAI configuration

The default chart does not deploy Ollama. OpenAI mode requires an API key:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set image.tag=<sha-tag> \
  --set genai.llmProvider=openai \
  --set genai.llmApiKey=<api-key>
```

For Rancher deployments, always pass the commit image tag, for example
`--set image.tag=sha-<commit>`. The chart default is `latest` for local
development, and using it manually can roll different services to a mutable tag.
Pass only the raw API key to `genai.llmApiKey`; do not paste `.env` lines such as
`LLM_API_KEY=...` or `LLM_MODEL=...`.

For infrastructure tests without an OpenAI key, use Ollama mode without deploying
Ollama. The GenAI service starts, but AI requests that need an Ollama backend may
fail until a backend is configured.

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml
```

To opt into in-cluster Ollama:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set ollama.enabled=true
```

## TLS and browser access

HTTP works through the Ingress by default. Browsers may force HTTPS for
`*.stud.k8s.aet.cit.tum.de` because of HSTS; if the cluster serves a self-signed
certificate, the browser can block access even though HTTP and `curl` checks work.

The chart supports TLS, but the correct cert-manager issuer must come from the
cluster admins or tutors:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set ingress.tls.enabled=true \
  --set ingress.tls.clusterIssuer=<issuer-name>
```
