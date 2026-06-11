# Team Drops Helm Chart

Deploys the Team Drops frontend, backend services, GenAI service, Postgres, and
MongoDB into an existing Rancher namespace.

The frontend image builds the Vite app into static files and serves them with
Nginx on port 80.

## Render and validate safely

```bash
helm lint ./helm/team-drops \
  -f helm/team-drops/values-rancher.yaml

helm template team-drops ./helm/team-drops \
  --namespace team-drops \
  -f helm/team-drops/values-rancher.yaml \
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
  permission to manage Helm releases and workloads in the `team-drops`
  namespace.

The workflow deploys the default image tag from `values.yaml`, currently
`latest`, and runs Helm with `--atomic --wait --timeout 10m`.

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
  --set genai.llmProvider=openai \
  --set genai.llmApiKey=<api-key>
```

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
