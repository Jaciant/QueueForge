# QueueForge v3.0.0 Release Checklist

Use this checklist before creating the Redis branch board cache release tag.

## 1. Local verification

Run the full test suite:

```bash
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

Run a clean application build:

```bash
./gradlew clean bootJar
```

On Windows:

```powershell
.\gradlew.bat clean bootJar
```

## 2. Docker verification

Build and start the full Docker environment:

```bash
docker compose -f docker-compose.app.yaml up --build
```

Verify the application starts successfully and Flyway applies all migrations.

Verify that the Docker environment starts:

- PostgreSQL
- Kafka
- Redis
- QueueForge application

Open Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Stop the environment:

```bash
docker compose -f docker-compose.app.yaml down
```

To remove PostgreSQL data as well:

```bash
docker compose -f docker-compose.app.yaml down -v
```

## 3. Manual API smoke test

Verify the core queue flow through Swagger UI, Postman, HTTP Client or curl:

1. Create an organization.
2. Create a branch.
3. Create queue services.
4. Create operator windows.
5. Assign services to an operator window.
6. Open the operator window.
7. Issue several tickets.
8. Call the next ticket.
9. Start service.
10. Complete or skip the ticket.
11. Check the branch board.
12. Check ticket status history.

## 4. Redis cache smoke test

Verify the branch board cache behavior in the Docker environment:

1. Open `GET /api/v1/branches/{branchId}/board` once to warm the Redis cache.
2. Issue a new ticket.
3. Open the branch board again and verify the new ticket is visible.
4. Call the next ticket.
5. Open the branch board again and verify the active ticket/window state is visible.
6. Start and complete the ticket.
7. Open the branch board again and verify the completed ticket no longer appears as active.

The important behavior is not only that Redis stores a board response, but that queue-changing commands evict the cached board before the next read.

## 5. Documentation check

Make sure README contains up-to-date information about:

- project purpose;
- tech stack;
- local launch;
- Docker launch;
- test execution;
- Swagger UI URL;
- ticket lifecycle;
- transactional outbox;
- Kafka dispatcher;
- Redis branch board cache;
- cache invalidation;
- architecture diagram;
- v3 scope;
- future roadmap.

## 6. GitHub CI check

Push the branch and verify that the CI workflow passes:

- Gradle build and tests;
- Testcontainers-based integration tests;
- Docker image build;
- Docker Compose config validation.

## 7. Create release tag

After all checks pass:

```bash
git tag v3.0.0
git push origin v3.0.0
```

Suggested release title:

```text
QueueForge v3.0.0 — Redis Branch Board Cache
```

Suggested release notes:

```text
QueueForge v3.0.0 adds Redis-backed branch board caching to the electronic queue management backend.

Included:
- organization, branch, queue service and operator window management;
- operator window service assignments;
- ticket issuing with atomic per-day sequence generation;
- call-next flow with PostgreSQL row locking;
- ticket lifecycle management;
- branch board read model;
- transactional outbox table;
- ticket lifecycle outbox events;
- outbox publisher with retry handling;
- Kafka dispatcher for outbox events;
- Redis branch board cache;
- cache-aside board reads with TTL;
- cache invalidation after ticket, queue service and operator window changes;
- Kafka integration test with Testcontainers;
- Redis cache and cache invalidation integration tests with Testcontainers;
- Flyway database migrations;
- integration and concurrency tests with Testcontainers;
- Swagger / OpenAPI documentation;
- Docker runtime with PostgreSQL, Kafka and Redis.
```

## 8. Post-release branch

After the release, create a development branch for the next iteration:

```bash
git checkout -b feature/v4-observability
```
