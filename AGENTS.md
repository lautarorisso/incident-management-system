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