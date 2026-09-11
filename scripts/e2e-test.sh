#!/usr/bin/env bash
# =============================================================================
# Incident Management System — E2E Test Runner
# =============================================================================
# Brings up the full docker-compose stack (idempotent), waits for every service
# to report healthy, then runs the black-box REST Assured suite in e2e-tests/
# against the real stack (./mvnw -pl e2e-tests test -DskipE2E=false).
#
# Cold-start note: services fetch central config from config-server via Eureka
# discovery, which races config-server's own registration on a fresh boot.
# The first health wait can therefore still find a service DOWN (usually the
# gateway, which auto-restarts thanks to `restart: unless-stopped` in
# docker-compose.yml). The launcher restarts any service that has not reported
# healthy after the first wait and re-checks before giving up.
#
# The stack is LEFT RUNNING on success so that scripts/smoke-test.sh can be
# used right after; tear it down manually with `docker compose down`.
#
# Usage:
#   ./scripts/e2e-test.sh              # default (local ports)
#
# Optional env vars (same surface as the E2E suite / smoke-test.sh):
#   GATEWAY_URL, INCIDENT_URL, NOTIFICATION_URL, USER_URL, DISCOVERY_URL,
#   KEYCLOAK_URL, KEYCLOAK_REALM, KEYCLOAK_CLIENT, E2E_USER, E2E_PASSWORD
# =============================================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# ---- Configuration ----------------------------------------------------------
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
INCIDENT_URL="${INCIDENT_URL:-http://localhost:8081}"
NOTIFICATION_URL="${NOTIFICATION_URL:-http://localhost:8083}"
USER_URL="${USER_URL:-http://localhost:8082}"
DISCOVERY_URL="${DISCOVERY_URL:-http://localhost:8761}"

# Keycloak knobs (used by the E2E suite via the environment, documented here
# for parity with smoke-test.sh).
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:18080}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-ims}"
KEYCLOAK_CLIENT="${KEYCLOAK_CLIENT:-ims-frontend}"
E2E_USER="${E2E_USER:-agente1}"
E2E_PASSWORD="${E2E_PASSWORD:-agente1234}"

HEALTH_TIMEOUT_SEC="${HEALTH_TIMEOUT_SEC:-180}"
POLL_INTERVAL="${POLL_INTERVAL:-5}"

# Service entries: "Display Name|health URL|compose service name"
SERVICES=(
    "Discovery Service|${DISCOVERY_URL}|discovery-service"
    "API Gateway|${GATEWAY_URL}|api-gateway"
    "Incident Service|${INCIDENT_URL}|incident-service"
    "Notification Service|${NOTIFICATION_URL}|notification-service"
    "User Service|${USER_URL}|user-service"
)

# ---- Colors -----------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color
PASS="${GREEN}✓${NC}"
FAIL="${RED}✗${NC}"
INFO="${YELLOW}→${NC}"

# ---- Helpers ----------------------------------------------------------------
wait_for_service() {
    local name="$1"
    local url="$2"
    local max_attempts=$((HEALTH_TIMEOUT_SEC / POLL_INTERVAL))
    local attempt=0

    echo -e "  ${INFO} Waiting for ${name} (${url}) ..."
    while [ "$attempt" -lt "$max_attempts" ]; do
        if curl -sf "${url}/actuator/health" &>/dev/null; then
            echo -e "  ${PASS} ${name} is healthy"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep "$POLL_INTERVAL"
    done
    echo -e "  ${FAIL} ${name} not healthy after ${HEALTH_TIMEOUT_SEC}s"
    return 1
}

# Waits for every service; leaves the entries that never became healthy in
# the global FAILED_ENTRIES array (progress is printed directly to stdout).
FAILED_ENTRIES=()
wait_for_all() {
    FAILED_ENTRIES=()
    local entry name url
    for entry in "${SERVICES[@]}"; do
        name="${entry%%|*}"
        url="$(echo "$entry" | cut -d'|' -f2)"
        if ! wait_for_service "$name" "$url"; then
            FAILED_ENTRIES+=("$entry")
        fi
    done
}

restart_services() {
    local services=()
    local entry
    for entry in "$@"; do
        services+=("$(echo "$entry" | cut -d'|' -f3)")
    done
    echo -e "  ${INFO} Restarting: ${services[*]}"
    docker compose restart "${services[@]}"
}

# ---- Main -------------------------------------------------------------------
main() {
    if ! command -v curl &>/dev/null; then
        echo "ERROR: curl is required but not installed"
        exit 1
    fi
    if ! command -v docker &>/dev/null; then
        echo "ERROR: docker is required but not installed"
        exit 1
    fi

    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  IMS E2E Test Suite"
    echo "  $(date '+%Y-%m-%d %H:%M:%S')"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""

    # ---- Phase 1: Bring up the stack ----------------------------------------
    echo "── Phase 1: Starting docker-compose stack ───────────────────────────"
    echo "  ${INFO} docker compose up -d --build (no-op when already up)"
    docker compose up -d --build
    echo ""

    # ---- Phase 2: Health gates (with one recovery pass) ---------------------
    echo "── Phase 2: Service health gates ────────────────────────────────────"
    wait_for_all
    echo ""

    if [ "${#FAILED_ENTRIES[@]}" -gt 0 ]; then
        echo -e "  ${INFO} Some services did not become healthy on the first pass"
        echo -e "  ${INFO} (cold-start config/discovery race) — restarting and re-checking ..."
        restart_services "${FAILED_ENTRIES[@]}"
        echo ""
        wait_for_all
        echo ""
    fi

    if [ "${#FAILED_ENTRIES[@]}" -gt 0 ]; then
        echo -e "  ${FAIL} One or more services are still unhealthy — aborting E2E run"
        exit 1
    fi

    # ---- Phase 3: Run the E2E suite ------------------------------------------
    echo "── Phase 3: Running REST Assured E2E suite (e2e-tests module) ───────"
    ./mvnw -pl e2e-tests test -DskipE2E=false
    echo ""

    echo "═══════════════════════════════════════════════════════════════"
    echo -e "  ${PASS} E2E suite passed — $(date '+%Y-%m-%d %H:%M:%S')"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "  The docker-compose stack is still running."
    echo "  - Run ./scripts/smoke-test.sh for the lightweight manual smoke test."
    echo "  - Tear the stack down with: docker compose down"
}

main "$@"