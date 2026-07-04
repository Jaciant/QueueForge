# QueueForge

QueueForge is a backend application for electronic queue management. The project models a real queue system for offices, service centers, banks, government branches or similar organizations where clients receive tickets, wait in a queue and are called to operator windows.

The current version is a monolithic Spring Boot MVP focused on correct business flow, transactional consistency and database-level concurrency safety.

## What the project demonstrates

QueueForge is not just a CRUD demo. The project includes a complete queue lifecycle:

```text
Organization -> Branch -> QueueService -> OperatorWindow
                                     -> Ticket
                                     -> Call next
                                     -> Ticket lifecycle
                                     -> Branch board
```

Core backend topics covered by the project:

- REST API design with Spring Web MVC
- layered architecture: controller, service, repository, entity, DTO
- PostgreSQL schema management with Flyway migrations
- JPA for ordinary entity operations
- native SQL / JdbcTemplate for database-specific atomic operations
- locking for concurrent business operations
- `FOR UPDATE SKIP LOCKED` for safe parallel ticket calling
- integration testing with Testcontainers PostgreSQL
- OpenAPI / Swagger documentation

## Tech stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- PostgreSQL 16
- Flyway
- Lombok
- Testcontainers
- JUnit 5
- Swagger / OpenAPI via springdoc-openapi
- Gradle
- Docker Compose

## Domain model

### Organization

Top-level business entity. For example:

```text
MFC
T-Bank Office Network
Medical Center
```

An organization owns branches.

### Branch

A physical office or service location. Branches belong to organizations and have their own timezone and status.

Example:

```text
MFC on Lenina Street
T-Bank Saint Petersburg Office
```

### Queue Service

A service available inside a branch.

Examples:

```text
PASSPORT
TAX
CONSULTATION
CARD_ISSUE
```

Tickets are issued for a specific queue service.

### Operator Window

A physical or logical operator workplace inside a branch.

Examples:

```text
Window 1
Window 2
Consultation Desk
```

Each window can be opened, paused or closed. Windows can also be assigned to specific queue services.

### Ticket

A ticket represents a client in the queue.

A ticket belongs to:

```text
Branch + QueueService + BusinessDate
```

Ticket numbers are generated per branch, service and date:

```text
PASSPORT-001
PASSPORT-002
TAX-001
```

## Ticket lifecycle

```text
WAITING -> CALLED -> IN_SERVICE -> COMPLETED
WAITING -> CANCELLED
CALLED  -> CANCELLED
CALLED  -> SKIPPED
```

Invalid transitions are rejected. For example, a `WAITING` ticket cannot be completed directly.

## Operator window lifecycle

```text
CLOSED -> OPEN
OPEN   -> PAUSED
PAUSED -> OPEN
OPEN   -> CLOSED
PAUSED -> CLOSED
```

Only `OPEN` windows can call the next ticket.

## API overview

### Organizations

```http
POST /api/v1/organizations
GET  /api/v1/organizations
GET  /api/v1/organizations/{organizationId}
```

### Branches

```http
POST  /api/v1/organizations/{organizationId}/branches
GET   /api/v1/organizations/{organizationId}/branches
GET   /api/v1/branches/{branchId}
PATCH /api/v1/branches/{branchId}/disable
PATCH /api/v1/branches/{branchId}/enable
```

### Queue services

```http
POST  /api/v1/branches/{branchId}/services
GET   /api/v1/branches/{branchId}/services
GET   /api/v1/services/{serviceId}
PATCH /api/v1/services/{serviceId}/disable
PATCH /api/v1/services/{serviceId}/enable
```

### Operator windows

```http
POST  /api/v1/branches/{branchId}/operator-windows
GET   /api/v1/branches/{branchId}/operator-windows
GET   /api/v1/operator-windows/{windowId}
PATCH /api/v1/operator-windows/{windowId}/open
PATCH /api/v1/operator-windows/{windowId}/pause
PATCH /api/v1/operator-windows/{windowId}/close
```

### Operator window service assignments

```http
PUT /api/v1/operator-windows/{windowId}/services
GET /api/v1/operator-windows/{windowId}/services
```

A window can call tickets only for services assigned to this window.

### Tickets

```http
POST  /api/v1/tickets
GET   /api/v1/tickets/{ticketId}
GET   /api/v1/branches/{branchId}/tickets/waiting
PATCH /api/v1/tickets/{ticketId}/start-service
PATCH /api/v1/tickets/{ticketId}/complete
PATCH /api/v1/tickets/{ticketId}/skip
PATCH /api/v1/tickets/{ticketId}/cancel
GET   /api/v1/tickets/{ticketId}/history
```

### Call next

```http
POST /api/v1/operator-windows/{windowId}/call-next
POST /api/v1/operator-windows/{windowId}/call-next?serviceId={serviceId}
```

The next ticket is selected by:

```text
priority desc, created_at asc
```

### Branch board

```http
GET /api/v1/branches/{branchId}/board
```

Returns a read-only view of the branch queue state:

- branch status
- services
- waiting count per service
- next waiting ticket per service
- full list of waiting tickets
- operator windows
- assigned services per window
- current ticket per window
- active tickets

## Concurrency model

QueueForge contains several concurrency-sensitive operations.

### Ticket number generation

Ticket sequence numbers are generated atomically using PostgreSQL:

```sql
insert into ticket_number_counters (...)
values (...)
on conflict (...)
do update set last_number = ticket_number_counters.last_number + 1
returning last_number
```

This avoids duplicate ticket numbers under parallel `POST /api/v1/tickets` requests.

### Calling the next ticket

When multiple windows call tickets concurrently, the system uses:

```sql
FOR UPDATE SKIP LOCKED
```

This prevents two windows from calling the same waiting ticket.

### Single active ticket per window

The operator window row is locked with pessimistic locking before calling the next ticket. This prevents one window from receiving two active tickets at the same time.

## Running locally

### Requirements

- Java 21
- Docker Desktop
- Gradle Wrapper from the repository

### Start PostgreSQL

```bash
docker compose up -d postgres
```

The database is exposed on port `5433`:

```text
jdbc:postgresql://127.0.0.1:5433/queueforge
```

Default credentials:

```text
username: queueforge
password: queueforge
```

### Run the application

```bash
./gradlew bootRun
```

On Windows:

```powershell
.\gradlew.bat bootRun
```

The application starts on:

```text
http://localhost:8080
```

## Swagger / OpenAPI

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

## Test execution

Run all tests:

```bash
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

Run only queue flow integration tests:

```powershell
.\gradlew.bat test --tests "*QueueFlowIntegrationTest"
```

Run only concurrency integration tests:

```powershell
.\gradlew.bat test --tests "*QueueConcurrencyIntegrationTest"
```

Integration tests use Testcontainers and start a temporary PostgreSQL container. They do not use the local development database.

## Example flow

### 1. Create organization

```http
POST /api/v1/organizations
Content-Type: application/json

{
  "name": "MFC"
}
```

### 2. Create branch

```http
POST /api/v1/organizations/{organizationId}/branches
Content-Type: application/json

{
  "name": "MFC on Lenina Street",
  "address": "Lenina Street, 10",
  "timezone": "Europe/Moscow"
}
```

### 3. Create queue service

```http
POST /api/v1/branches/{branchId}/services
Content-Type: application/json

{
  "code": "PASSPORT",
  "name": "Passport issue",
  "description": "Passport issue and replacement",
  "avgServiceTimeMinutes": 15
}
```

### 4. Create operator window

```http
POST /api/v1/branches/{branchId}/operator-windows
Content-Type: application/json

{
  "number": 1,
  "name": "Window 1"
}
```

### 5. Assign services to window

```http
PUT /api/v1/operator-windows/{windowId}/services
Content-Type: application/json

{
  "serviceIds": [
    "{serviceId}"
  ]
}
```

### 6. Open window

```http
PATCH /api/v1/operator-windows/{windowId}/open
```

### 7. Issue ticket

```http
POST /api/v1/tickets
Content-Type: application/json

{
  "branchId": "{branchId}",
  "serviceId": "{serviceId}",
  "priority": 0
}
```

### 8. Call next ticket

```http
POST /api/v1/operator-windows/{windowId}/call-next
```

### 9. Start service

```http
PATCH /api/v1/tickets/{ticketId}/start-service
```

### 10. Complete ticket

```http
PATCH /api/v1/tickets/{ticketId}/complete
```

### 11. Check branch board

```http
GET /api/v1/branches/{branchId}/board
```

## Project structure

```text
src/main/java/com/ldpst/queueforge
├── board              # read-only branch board API
├── branch             # branch management
├── common             # shared exceptions and configuration
├── operatorwindow     # operator window management and service assignments
├── organization       # organization management
├── queueservice       # queue service catalog
└── ticket             # ticket issue, call-next and lifecycle
```

## Current release scope

The first release focuses on a stable monolithic MVP:

- complete queue business flow
- PostgreSQL-backed persistence
- Flyway migrations
- integration tests
- concurrency tests
- Swagger documentation
- local development via Docker Compose

## Planned next versions

### v2: asynchronous events

- domain events
- Kafka integration
- transactional outbox pattern
- event publisher

### v3: caching and performance

- Redis cache for read models
- queue board caching
- rate limiting
- load testing

### v4: observability

- custom metrics
- Prometheus integration
- Grafana dashboard
- structured logging
