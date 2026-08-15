# POC - Corporate Training Platform

Microservices architecture for corporate training management based on Spring Boot, Kubernetes and cloud-native technologies.

## Tech Stack

- **Java 21+**
- **Spring Boot 3.4**
- **Gradle**
- **PostgreSQL**
- **RabbitMQ**
- **Keycloak**
- **Kong Gateway**
- **Prometheus + Jaeger + Loki + Grafana**
- **OpenTelemetry**
- **Kubernetes**

## Microservices

| Service | Port | Description |
|---|---|---|
| training-catalog-service | 8081 | Course management |
| enrollment-service | 8082 | Employee enrollment |
| certification-service | 8083 | Certificate issuance |
| notification-service | 8084 | Automatic notifications |
| employee-service | 8085 | Employee records management |

## Infrastructure

| Component | Port | URL |
|---|---|---|
| PostgreSQL | 5432 | - |
| Keycloak | 8080 | http://localhost:8080 |
| RabbitMQ | 5672/15672 | http://localhost:15672 |
| Kong | 8000/8001 | http://localhost:8000 |
| Prometheus | 9090 | http://localhost:9090 |
| Jaeger | 16686 | http://localhost:16686 |
| Loki | 3100 | - |
| Grafana | 3000 | http://localhost:3000 |

## Quick Start

### 1. Start infrastructure

```bash
rake infra:start
```

### 2. Run tests

```bash
rake test:run
```

### 3. View all available commands

```bash
rake help
```

## Test Users

| User | Password | Role |
|---|---|---|
| admin | admin | admin |
| instructor1 | instructor1 | instructor |
| employee1 | employee1 | employee |

## API Endpoints

### Training Catalog Service

```
GET    /api/v1/courses           - List courses
GET    /api/v1/courses/{id}      - Get course
POST   /api/v1/courses           - Create course
PUT    /api/v1/courses/{id}      - Update course
DELETE /api/v1/courses/{id}      - Deactivate course
```

## Business Events

- `EnrollmentCreated`
- `EnrollmentApproved`
- `CourseCompleted`
- `CertificateGenerated`
- `NotificationSent`

## License

MIT License

## Documentation

- [Architecture](docs/architecture.md)
- [Acceptance Criteria](docs/acceptance-criteria.md)
