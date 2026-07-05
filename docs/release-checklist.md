# QueueForge v2.0.0 Release Checklist

Use this checklist before creating the asynchronous events release tag.

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

## 4. Documentation check

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
- architecture diagram;
- v2 scope;
- future roadmap.

## 5. GitHub CI check

Push the branch and verify that the CI workflow passes:

- Gradle build and tests;
- Testcontainers-based integration tests;
- Docker image build;
- Docker Compose config validation.

## 6. Create release tag

After all checks pass:

```bash
git tag v2.0.0
git push origin v2.0.0
```

Suggested release title:

```text
QueueForge v2.0.0 — Transactional Outbox and Kafka Events
```

Suggested release notes:

```text
QueueForge v2.0.0 adds reliable asynchronous event publishing to the electronic queue management backend.

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
- Kafka integration test with Testcontainers;
- Flyway database migrations;
- integration and concurrency tests with Testcontainers;
- Swagger / OpenAPI documentation;
- Docker runtime with PostgreSQL and Kafka.
```

## 7. Post-release branch

After the release, create a development branch for the next iteration:

```bash
git checkout -b feature/v3-redis-cache
```
