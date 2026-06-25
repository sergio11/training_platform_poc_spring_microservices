# Architecture - Corporate Training Platform

## Overview

This document describes the architecture of the Corporate Training Platform, a microservices-based system for managing corporate training, enrollments, certifications, and notifications.

## System Architecture

```
                                    ┌─────────────────┐
                                    │   Kong Gateway   │
                                    │    (Port 8000)   │
                                    └────────┬────────┘
                                             │
            ┌────────────────────────────────┼────────────────────────────────┐
            │                                │                                │
            ▼                                ▼                                ▼
┌───────────────────┐            ┌───────────────────┐            ┌───────────────────┐
│  Training Catalog │            │    Enrollment     │            │   Certification   │
│     Service       │            │     Service       │            │     Service       │
│    (Port 8081)    │            │    (Port 8082)    │            │    (Port 8083)    │
└─────────┬─────────┘            └─────────┬─────────┘            └─────────┬─────────┘
          │                                │                                │
          │                                │                                │
          └────────────────────────────────┼────────────────────────────────┘
                                           │
                                           ▼
                                ┌───────────────────┐
                                │   Notification    │
                                │     Service       │
                                │    (Port 8084)    │
                                └───────────────────┘
```

## Microservices

### Training Catalog Service (Port 8081)
- **Purpose**: Manages course catalog
- **Responsibilities**:
  - CRUD operations for courses
  - Course capacity management
  - Course status management
- **Endpoints**:
  - `GET /api/v1/courses` - List all courses
  - `GET /api/v1/courses/{id}` - Get course by ID
  - `POST /api/v1/courses` - Create course
  - `PUT /api/v1/courses/{id}` - Update course
  - `DELETE /api/v1/courses/{id}` - Deactivate course

### Enrollment Service (Port 8082)
- **Purpose**: Manages employee enrollments
- **Responsibilities**:
  - Employee enrollment in courses
  - Seat availability validation
  - Enrollment approval/completion
  - Resilience4j circuit breaker for external calls
- **Endpoints**:
  - `GET /api/v1/enrollments?employeeId=X` - List enrollments
  - `GET /api/v1/enrollments/{id}` - Get enrollment
  - `POST /api/v1/enrollments?employeeId=X&courseId=Y` - Create enrollment
  - `PATCH /api/v1/enrollments/{id}/approve` - Approve enrollment
  - `PATCH /api/v1/enrollments/{id}/complete` - Complete enrollment

### Certification Service (Port 8083)
- **Purpose**: Manages course certificates
- **Responsibilities**:
  - Automatic certificate generation on course completion
  - Certificate number generation
  - Certificate validation
- **Endpoints**:
  - `GET /api/v1/certificates?employeeId=X` - List certificates
  - `GET /api/v1/certificates/{id}` - Get certificate
  - `GET /api/v1/certificates/number/{num}` - Get by certificate number

### Notification Service (Port 8084)
- **Purpose**: Sends notifications for business events
- **Responsibilities**:
  - Listen to business events via RabbitMQ
  - Send notifications for enrollments, completions, certificates
  - Store notification history
- **Endpoints**:
  - `GET /api/v1/notifications?recipientId=X` - List notifications
  - `GET /api/v1/notifications/{id}` - Get notification

## Event Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           BUSINESS EVENT FLOW                               │
└─────────────────────────────────────────────────────────────────────────────┘

1. Enrollment Created
   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
   │ Enrollment      │───▶│  RabbitMQ       │───▶│  Notification   │
   │ Service         │    │  (enrollment.   │    │  Service        │
   │                 │    │   created)      │    │                 │
   └─────────────────┘    └─────────────────┘    └─────────────────┘

2. Enrollment Approved
   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
   │ Enrollment      │───▶│  RabbitMQ       │───▶│  Notification   │
   │ Service         │    │  (enrollment.   │    │  Service        │
   │                 │    │   approved)     │    │                 │
   └─────────────────┘    └─────────────────┘    └─────────────────┘

3. Course Completed
   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
   │ Enrollment      │───▶│  RabbitMQ       │───▶│  Certification  │
   │ Service         │    │  (course.       │    │  Service        │
   │                 │    │   completed)    │    │                 │
   └─────────────────┘    └─────────────────┘    └────────┬────────┘
                                                          │
                                                          ▼
                                               ┌─────────────────┐
                                               │  Generates      │
                                               │  Certificate    │
                                               └────────┬────────┘
                                                        │
                                                        ▼
                                               ┌─────────────────┐
                                               │  RabbitMQ       │
                                               │  (certificate.  │
                                               │   generated)    │
                                               └────────┬────────┘
                                                        │
                                                        ▼
                                               ┌─────────────────┐
                                               │  Notification   │
                                               │  Service        │
                                               └─────────────────┘

4. Notification Sent
   ┌─────────────────┐    ┌─────────────────┐
   │ Notification    │───▶│  RabbitMQ       │
   │ Service         │    │  (notification. │
   │                 │    │   sent)         │
   └─────────────────┘    └─────────────────┘
```

## Infrastructure Components

### PostgreSQL
- **Purpose**: Primary database for all services
- **Databases**:
  - `training_catalog` - Catalog Service
  - `training_enrollment` - Enrollment Service
  - `training_certification` - Certification Service
  - `training_notification` - Notification Service

### RabbitMQ
- **Purpose**: Asynchronous messaging between services
- **Exchange**: `training.exchange` (topic)
- **Queues**:
  - `enrollment.created`
  - `enrollment.approved`
  - `course.completed`
  - `certificate.generated`
  - `notification.sent`

### Keycloak
- **Purpose**: OAuth2/OIDC authentication and authorization
- **Realm**: `training-realm`
- **Clients**:
  - `training-catalog-client`
  - `training-enrollment-client`
  - `training-certification-client`
  - `training-notification-client`
  - `training-frontend`

### Kong Gateway
- **Purpose**: API Gateway, routing and rate limiting
- **Routes**:
  - `/api/v1/courses` → Training Catalog Service
  - `/api/v1/enrollments` → Enrollment Service
  - `/api/v1/certificates` → Certification Service
  - `/api/v1/notifications` → Notification Service

### Config Server
- **Purpose**: Centralized configuration management
- **Port**: 8888
- **Config files**: `application.yml` for each service

## Observability

### Prometheus
- **Purpose**: Metrics collection
- **Port**: 9090
- **Scrape targets**: All microservices via Actuator

### Jaeger
- **Purpose**: Distributed tracing
- **Port**: 16686
- **Protocol**: OpenTelemetry (OTLP)

### Loki
- **Purpose**: Log aggregation
- **Port**: 3100
- **Integration**: OpenTelemetry Collector

### Grafana
- **Purpose**: Dashboards and visualization
- **Port**: 3000
- **Datasources**: Prometheus, Jaeger, Loki

## Resilience Patterns

### Circuit Breaker (Resilience4j)
- **Service**: Enrollment Service → Training Catalog Service
- **Configuration**:
  - Sliding window: 10 calls
  - Failure threshold: 50%
  - Wait duration: 10 seconds
  - Half-open calls: 3

### Retry (Resilience4j)
- **Service**: Enrollment Service → Training Catalog Service
- **Configuration**:
  - Max attempts: 3
  - Wait duration: 500ms

## Security

### Authentication
- **Provider**: Keycloak
- **Protocol**: OAuth2/OIDC
- **Token type**: JWT

### Authorization
- **Resource Server**: All services validate JWT tokens
- **Roles**: admin, instructor, employee

## Deployment

### Local Development
```bash
# Start infrastructure
rake infra:start

# Build all services
rake build:all

# Run specific service
rake run:service[training-catalog-service]
```

### Kubernetes Deployment
```bash
# Deploy all
rake k8s:deploy

# Check status
rake k8s:status

# View logs
rake k8s:logs[training-catalog-service]
```

## Port Summary

| Service | Port |
|---|---|
| Training Catalog Service | 8081 |
| Enrollment Service | 8082 |
| Certification Service | 8083 |
| Notification Service | 8084 |
| Config Server | 8888 |
| PostgreSQL | 5432 |
| RabbitMQ | 5672, 15672 |
| Keycloak | 8080 |
| Kong Gateway | 8000, 8001 |
| Prometheus | 9090 |
| Jaeger | 16686 |
| Loki | 3100 |
| Grafana | 3000 |
