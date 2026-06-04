# Team Drops Helm Chart

Deploys the Team Drops frontend, backend services, GenAI service, Postgres, and
MongoDB into an existing Rancher namespace.

The frontend image builds the Vite app into static files and serves them with
Nginx on port 80.

## Render and validate safely

```bash
helm lint ./helm/team-drops \
  --set ingress.host=test.stud.k8s.aet.cit.tum.de \
  --set genai.llmApiKey=dummy

helm template team-drops ./helm/team-drops \
  --namespace team-drops \
  --set ingress.host=test.stud.k8s.aet.cit.tum.de \
  --set genai.llmApiKey=dummy \
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

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  --set image.tag=kubernetes \
  --set ingress.host=team-drops.stud.k8s.aet.cit.tum.de \
  --set genai.llmProvider=ollama
```

If GHCR packages are private, create an image pull secret in the namespace and
install with:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  --set ingress.host=<your-name>.stud.k8s.aet.cit.tum.de \
  --set genai.llmApiKey=<api-key> \
  --set imagePullSecrets[0]=<secret-name>
```

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
  --set ingress.host=<your-name>.stud.k8s.aet.cit.tum.de \
  --set genai.llmProvider=openai \
  --set genai.llmApiKey=<api-key>
```

For infrastructure tests without an OpenAI key, use Ollama mode without deploying
Ollama. The GenAI service starts, but AI requests that need an Ollama backend may
fail until a backend is configured.

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  --set ingress.host=<your-name>.stud.k8s.aet.cit.tum.de \
  --set genai.llmProvider=ollama
```

To opt into in-cluster Ollama:

```bash
helm upgrade --install team-drops ./helm/team-drops \
  --namespace team-drops \
  --set ingress.host=<your-name>.stud.k8s.aet.cit.tum.de \
  --set genai.llmProvider=ollama \
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
  --set ingress.host=<your-name>.stud.k8s.aet.cit.tum.de \
  --set genai.llmProvider=ollama \
  --set ingress.tls.enabled=true \
  --set ingress.tls.clusterIssuer=<issuer-name>
```
