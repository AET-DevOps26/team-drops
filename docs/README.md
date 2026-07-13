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

- [API inventory](API_list.md)
- [Frontend/backend API integration](frontend-backend-api-integration.md)
- [Central OpenAPI contract](../api/openapi.yaml)
- [Per-service OpenAPI contracts](../api/services/)

## Authentication and gateway

- [Keycloak authentication](keycloak-authentication.md)
- [Nginx gateway](nginx-gateway.md)
- [Local Keycloak realm export](../keycloak/realm-export.json)

## Deployment and observability

- [Application and monitoring Helm operations](../helm/team-drops/README.md)
- [Application Helm chart](../helm/team-drops/)
- [Monitoring Helm chart](../helm/team-drops-monitoring/)
- [Grafana dashboards as code](../helm/team-drops-monitoring/dashboards/)

The monitoring operations guide covers Prometheus metrics, Grafana dashboards,
Loki logs, Alloy collection, Alertmanager routing, persistence, verification,
and rollback.

## Historical project material

- [Project proposal](Project%20Proposal.pdf)
- [Product backlog](https://github.com/AET-DevOps26/team-drops/wiki/Product-Backlog)
- [Product-backlog user stories](https://github.com/AET-DevOps26/team-drops/wiki/Product-Backlog:-User-Stories)

These files provide planning context and are not the source of truth for the
current runtime or deployment configuration.
