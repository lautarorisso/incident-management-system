#!/usr/bin/env bash
# =============================================================================
# Incident Management System — Test Runner
# =============================================================================
# Runs the Maven test suite. Tests require Docker (Testcontainers spins up
# PostgreSQL 16, MongoDB 7 and RabbitMQ 3) and Java 21.
#
# Usage:
#   ./scripts/test.sh test                        # full suite (all modules)
#   ./scripts/test.sh test -pl services/notification-service -am
# =============================================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"
if [ "$#" -eq 0 ]; then
    set -- test
fi
exec ./mvnw "$@"