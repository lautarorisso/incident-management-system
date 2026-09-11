# Incident Management System

## Environment notes

### Running tests

Tests require Docker (Testcontainers spins up PostgreSQL 16, MongoDB 7 and
RabbitMQ 3) and Java 21. Run the suite with `./scripts/test.sh`, a thin wrapper
that passes every argument straight through to `./mvnw`:

```sh
./scripts/test.sh test                          # full suite (all modules)
./scripts/test.sh test -pl services/notification-service -am
./scripts/test.sh test -pl services/incident-service -am
```

### E2E tests (REST Assured)

The `e2e-tests/` module is a black-box RestAssured + JUnit 5 suite (no Spring
context, no project-internal dependencies) that runs against the real
docker-compose stack. It is part of the reactor and **compiles** in the fast
suite, but its tests are gated by the `skipE2E` property (surefire
`skipTests`, default `true`) — the normal run above executes zero E2E tests.

Run the full E2E cycle (stack up + health gates + suite; stack left running):

```sh
./scripts/e2e-test.sh
```

Suite only (stack must already be up):

```sh
./mvnw -pl e2e-tests test -DskipE2E=false
```