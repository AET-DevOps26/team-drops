# Team Drops Helm Chart

Deploys the Team Drops frontend, backend services, GenAI service, Postgres, and
MongoDB into an existing Rancher namespace.

The frontend image builds the Vite app into static files and serves them with
Nginx on port 80.

## Render and validate safely

```bash
helm dependency build ./helm/team-drops

helm lint ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set genai.llmApiKey=dummy \
  --set monitoring.standalone.grafanaAdminPassword=dummy

helm template team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set genai.llmApiKey=dummy \
  --set monitoring.standalone.grafanaAdminPassword=dummy \
  > /tmp/team-drops-rendered.yaml
```

Inspect the rendered manifests:

```bash
grep -n "image:" /tmp/team-drops-rendered.yaml
grep -n "host:" /tmp/team-drops-rendered.yaml
grep -n "storage:" /tmp/team-drops-rendered.yaml
grep -n "namespace:" /tmp/team-drops-rendered.yaml
```

Expected:

- app images point to `ghcr.io/aet-devops26/...`
- ingress hosts use `*.stud.k8s.aet.cit.tum.de`
- Postgres and Mongo storage defaults are small
- `namespace:` has no matches because Helm receives the namespace at install time
- backend env vars use either `value` or `valueFrom`, never both

Validate against the Kubernetes API without creating resources:

```bash
kubectl apply --dry-run=server -n team-drops -f /tmp/team-drops-rendered.yaml
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
  --set image.tag=kubernetes \
  --set monitoring.standalone.grafanaAdminPassword=<password>
```

If GHCR packages are private, create an image pull secret in the namespace and
install with:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set imagePullSecrets[0]=<secret-name> \
  --set monitoring.standalone.grafanaAdminPassword=<password>
```

## Automatic deployment

The `Deploy Kubernetes` GitHub Actions workflow deploys this chart after the
`Docker Publish` workflow succeeds on `main`. It can also be started manually
from the Actions tab.

Required repository secret:

- `KUBE_CONFIG`: kubeconfig content for a Rancher user or service account with
  permission to manage Helm releases and workloads in the `team-drops`
  namespace.
- `GRAFANA_ADMIN_PASSWORD`: administrator password for the namespace-owned
  Grafana instance; it is written only to the Kubernetes Secret.

Automatic runs deploy the immutable `sha-<commit>` image tag published by the
`Docker Publish` workflow. Manual runs default to `latest`, but allow choosing a
specific image tag from the Actions tab. The workflow runs Helm with
`--rollback-on-failure --wait --timeout 10m`.

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

Internal health checks via port-forward:

```bash
kubectl -n team-drops port-forward svc/user-service 8081:80
curl http://localhost:8081/actuator/health
```

```bash
kubectl -n team-drops port-forward svc/genai-service 8084:80
curl http://localhost:8084/health
```

## Prometheus and Grafana monitoring

The Rancher values deploy a private Prometheus and Grafana stack inside the
`team-drops` namespace. Prometheus retains seven days of data on a 2 GiB PVC,
Grafana uses a 1 GiB PVC, and discovery is restricted by namespaced RBAC to API
Services labeled `monitoring: "true"`. Neither UI is exposed through Ingress.

Check the stack and persistent storage:

```bash
kubectl -n team-drops get pods,svc,pvc | grep -E 'prometheus|grafana'
kubectl -n team-drops get role,rolebinding team-drops-prometheus
```

Access Grafana:

```bash
kubectl -n team-drops port-forward svc/team-drops-grafana 3000:80
```

Open `http://localhost:3000`, sign in as `admin` with the value of
`GRAFANA_ADMIN_PASSWORD`, and open **Dashboards > Team Drops > Team Drops
Overview**.

Access the Prometheus query and target UI:

```bash
kubectl -n team-drops port-forward svc/team-drops-prometheus-server 9090:80
```

Open `http://localhost:9090`. Useful queries are:

```promql
up{job="team-drops-services"}
application_info{namespace="team-drops"}
sum(rate(http_server_requests_seconds_count{namespace="team-drops"}[5m]))
sum(rate(http_requests_total{namespace="team-drops"}[5m]))
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
tag. Rancher `ServiceMonitor` resources remain available through
`monitoring.rancherServiceMonitors.enabled`, but are disabled in
`values-rancher.yaml` because this stack does not use the cluster Prometheus.

Rollback or uninstall:

```bash
helm history team-drops -n team-drops
helm rollback team-drops <revision> -n team-drops
helm uninstall team-drops -n team-drops
```

## GenAI configuration

The default chart does not deploy Ollama. OpenAI mode requires an API key:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set image.tag=<sha-tag> \
  --set genai.llmProvider=openai \
  --set genai.llmApiKey=<api-key> \
  --set monitoring.standalone.grafanaAdminPassword=<password>
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
  -f helm/team-drops/values-rancher.yaml \
  --set monitoring.standalone.grafanaAdminPassword=<password>
```

To opt into in-cluster Ollama:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
  --set monitoring.standalone.grafanaAdminPassword=<password> \
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
  --set monitoring.standalone.grafanaAdminPassword=<password> \
  --set ingress.tls.enabled=true \
  --set ingress.tls.clusterIssuer=<issuer-name>
```
