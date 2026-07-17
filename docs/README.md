# Team Drops Documentation

This directory contains the architecture, integration, authentication, and
historical project documents for Team Drops. Start with the repository
[README](../README.md) for setup and the
[Helm operations guide](../helm/team-drops/README.md) for deployment.

## Architecture and design

- [Top-level architecture diagram](top-level%20architecture%20diagram.pdf)
- [System structure](system%20structure.pdf)
- [Class diagram](Class%20Diagram.pdf)
- [Use-case diagram](Use%20Case%20Diagram.pdf)
- [Microservice best practices](microservice-best-practices.md)

## APIs and integration

- [Current API inventory](API_list.md)
- [Current frontend/backend integration](frontend-backend-api-integration.md)
- [Central OpenAPI contract](../api/openapi.yaml)
- [Per-service OpenAPI contracts](../api/services/)

## Authentication and gateway

- [Keycloak authentication](keycloak-authentication.md)
- [Nginx gateway](nginx-gateway.md)
- [Local Keycloak realm export](../keycloak/realm-export.json)

## Quality assurance

- [Testing strategy and coverage matrix](testing-strategy.md)
- [Automated test report — 2026-07-17](automated-test-report-2026-07-17.md)
- [Local authentication and AI test plan](local-auth-ai-test-plan.md)
- [Local authentication and AI test report](local-auth-ai-test-report.md)

## Deployment and observability

- [Application and monitoring Helm operations](../helm/team-drops/README.md)
- [Application Helm chart](../helm/team-drops/)
- [Monitoring Helm chart](../helm/team-drops-monitoring/)
- [Grafana dashboards as code](../helm/team-drops-monitoring/dashboards/)

The monitoring operations guide covers Prometheus metrics, Grafana dashboards,
Loki logs, Alloy collection, Alertmanager routing, persistence, verification,
least-privilege kube-state-metrics access, failed-release recovery, and
rollback.

## Historical project material

- [Project proposal](Project%20Proposal.pdf)
- [Product backlog](https://github.com/AET-DevOps26/team-drops/wiki/Product-Backlog)
- [Product-backlog user stories](https://github.com/AET-DevOps26/team-drops/wiki/Product-Backlog:-User-Stories)

These files provide planning context and are not the source of truth for the
current runtime or deployment configuration.
