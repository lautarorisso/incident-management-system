#!/usr/bin/env bash
# =============================================================================
# Incident Management System — E2E Test Runner
# =============================================================================
# Brings up the full docker-compose stack (idempotent), waits for every service
# to report healthy, verifies routing readiness (Eureka registrations + gateway
# route resolution), then runs the black-box REST Assured suite in e2e-tests/
# against the real stack (./mvnw -pl e2e-tests test -DskipE2E=false).
#
# Cold-start note: services fetch central config from config-server via Eureka
# discovery, which races config-server's own registration on a fresh boot.
# The first health wait can therefore still find a service DOWN (usually the
# gateway, which auto-restarts thanks to `restart: unless-stopped` in
# docker-compose.yml). The launcher restarts any service that has not reported
# healthy after the first wait and re-checks before giving up.
#
# Routing note: a healthy actuator status does not mean the gateway can route —
# its load balancer only resolves instances once Eureka registrations have been
# cached, which lags a fresh stack by tens of seconds. Until then, requests hit
# "No servers available" (503). The launcher therefore waits for all application
# services to appear in Eureka, obtains an OAuth2 token, and probes the gateway
# routes with it until they resolve (bounded by ROUTING_TIMEOUT_SEC, default
# 120s) before running the suite. Unauthenticated probes are useless here: the
# JWT filter rejects them (401) before the load balancer is ever consulted.
#
# Circuit-breaker note: the gateway records any 5xx route response as a circuit
# breaker failure (sliding window 10, min 5 calls, 50% threshold). Cold-start
# 503 probes therefore accumulate failures, and the first successful probe can
# open the circuit right after the routes resolve — the suite then gets 503s
# from the fallback until the breaker (30s open state) recovers. To avoid that
# race, the gate requires STABILITY_ROUNDS consecutive clean probe rounds (no
# 000, no 5xx): while a breaker is open, probe rounds keep failing until it
# transitions to half-open, and the clean round that gets through closes it
# again. The notification probe passes a well-formed but bogus userId so the
# owner check answers 403 (non-5xx) instead of 500 for a missing parameter.
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

# Routing-readiness gate knobs (Phase 3).
ROUTING_TIMEOUT_SEC="${ROUTING_TIMEOUT_SEC:-120}"
ROUTING_POLL_INTERVAL="${ROUTING_POLL_INTERVAL:-5}"

# Consecutive clean probe rounds (no 000, no 5xx) required before the gateway
# routes count as ready. Clean rounds keep the circuit breaker's sliding-window
# failure rate below the 50% threshold (see the header routing note).
STABILITY_ROUNDS="${STABILITY_ROUNDS:-3}"

# Application services that must appear in Eureka (/eureka/apps) before the
# gateway's load balancer can resolve them.
EUREKA_SERVICES=(
    "API-GATEWAY"
    "USER-SERVICE"
    "INCIDENT-SERVICE"
    "NOTIFICATION-SERVICE"
)

# Gateway routes probed until the load balancer has resolved downstream
# instances. Probes carry an OAuth2 bearer token: unauthenticated requests
# are rejected at the gateway's JWT filter (401) BEFORE the load balancer is
# consulted, so they cannot detect a cold routing cache. With a valid token,
# a warm route returns 200/403 (routing reached the service); 503 means no
# instance or an open circuit breaker; 000 means the gateway did not answer.
# The notification probe passes a well-formed but bogus userId: without it the
# service answers 500 for the missing required parameter, which would count as
# a circuit breaker failure; with it, the owner check answers 403 (non-5xx) as
# soon as routing works.
GATEWAY_ROUTES=(
    "${GATEWAY_URL}/api/incidents"
    "${GATEWAY_URL}/api/users"
    "${GATEWAY_URL}/api/notifications?userId=00000000-0000-0000-0000-000000000000"
)

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

# Waits until every application service appears in Eureka's registry. A
# healthy actuator status does not guarantee routing: the gateway only
# resolves instances once Eureka registrations are cached, which lags a fresh
# stack by tens of seconds. Prints progress with timestamps; exits 1 (via the
# caller) with the still-missing services when the timeout is reached.
wait_for_eureka_registrations() {
    local attempt=0
    local max_attempts=$((ROUTING_TIMEOUT_SEC / ROUTING_POLL_INTERVAL))
    local body=""
    local missing=""
    local name

    echo -e "  ${INFO} Waiting for Eureka registrations (${DISCOVERY_URL}/eureka/apps) ..."
    while [ "$attempt" -lt "$max_attempts" ]; do
        body="$(curl -sf "${DISCOVERY_URL}/eureka/apps" 2>/dev/null || true)"
        missing=""
        for name in "${EUREKA_SERVICES[@]}"; do
            if ! printf '%s' "$body" | grep -q ">${name}<"; then
                missing="${missing:+$missing }${name}"
            fi
        done
        if [ -z "$missing" ]; then
            echo -e "  ${PASS} All application services registered in Eureka"
            return 0
        fi
        echo "  [$(date '+%H:%M:%S')] waiting for Eureka registrations: ${missing}"
        attempt=$((attempt + 1))
        sleep "$ROUTING_POLL_INTERVAL"
    done
    echo -e "  ${FAIL} Services never registered in Eureka after ${ROUTING_TIMEOUT_SEC}s: ${missing}"
    return 1
}

# OAuth2 access token used by the gateway route probes (see the routing note
# in the header). Reuses the E2E suite's Keycloak knobs.
ACCESS_TOKEN=""

fetch_access_token() {
    local token_response
    token_response=$(curl -sf "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
        -d grant_type=password \
        -d client_id="${KEYCLOAK_CLIENT}" \
        -d username="${E2E_USER}" \
        -d password="${E2E_PASSWORD}" 2>/dev/null) || token_response=""

    if command -v jq &>/dev/null; then
        ACCESS_TOKEN=$(printf '%s' "$token_response" | jq -r '.access_token // empty' 2>/dev/null)
    else
        ACCESS_TOKEN=$(printf '%s' "$token_response" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
    fi

    if [ -z "$ACCESS_TOKEN" ]; then
        echo -e "  ${FAIL} Could not obtain a Keycloak token (${KEYCLOAK_URL}, realm ${KEYCLOAK_REALM}, user ${E2E_USER})"
        return 1
    fi
    echo -e "  ${PASS} Keycloak token obtained (${E2E_USER})"
}

# Waits until the gateway routes resolve downstream instances. Probes carry a
# bearer token: unauthenticated requests are rejected at the JWT filter (401)
# before the load balancer is consulted, so they cannot detect a cold route.
# Before the load balancer caches Eureka registrations, authenticated requests
# hit "No servers available" (503); once the circuit breaker has seen enough
# 5xx responses it opens and answers 503 from the fallback until it recovers
# (see the header routing note). A probe round is "clean" when every route
# answers non-5xx (2xx/3xx/4xx) and the gateway answered at all (not 000);
# STABILITY_ROUNDS consecutive clean rounds are required so that the breaker
# sliding window cannot flip to open right after the gate passes. Prints
# progress with timestamps; on timeout it reports the last observed status per
# route and exits 1 (via the caller).
wait_for_gateway_routes() {
    local attempt=0
    local max_attempts=$((ROUTING_TIMEOUT_SEC / ROUTING_POLL_INTERVAL))
    local clean_rounds=0
    local code=""
    local pending=""
    local route

    echo -e "  ${INFO} Waiting for gateway routes to resolve instances ..."
    while [ "$attempt" -lt "$max_attempts" ]; do
        pending=""
        for route in "${GATEWAY_ROUTES[@]}"; do
            code="$(curl -s -o /dev/null -w '%{http_code}' \
                -H "Authorization: Bearer ${ACCESS_TOKEN}" "${route}" 2>/dev/null || true)"
            if [ "$code" = "000" ] || { [ "$code" -ge 500 ] 2>/dev/null; }; then
                pending="${pending:+$pending }${route} → ${code}"
            fi
        done
        if [ -z "$pending" ]; then
            clean_rounds=$((clean_rounds + 1))
            if [ "$clean_rounds" -ge "$STABILITY_ROUNDS" ]; then
                echo -e "  ${PASS} Gateway routes resolve downstream instances"
                return 0
            fi
            echo "  [$(date '+%H:%M:%S')] routes stable ($clean_rounds/$STABILITY_ROUNDS rounds)"
        else
            clean_rounds=0
            echo "  [$(date '+%H:%M:%S')] routes not ready: ${pending}"
        fi
        attempt=$((attempt + 1))
        sleep "$ROUTING_POLL_INTERVAL"
    done
    echo -e "  ${FAIL} Gateway routes still unavailable after ${ROUTING_TIMEOUT_SEC}s — last observed statuses:"
    for route in "${GATEWAY_ROUTES[@]}"; do
        code="$(curl -s -o /dev/null -w '%{http_code}' \
            -H "Authorization: Bearer ${ACCESS_TOKEN}" "${route}" 2>/dev/null || true)"
        echo "    ${route} → ${code}"
    done
    return 1
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

    # ---- Phase 3: Routing-readiness gate -------------------------------------
    echo "── Phase 3: Routing-readiness gate ─────────────────────────────────"
    if ! wait_for_eureka_registrations; then
        echo -e "  ${FAIL} Eureka registrations incomplete — aborting E2E run"
        exit 1
    fi
    if ! fetch_access_token; then
        echo -e "  ${FAIL} Cannot obtain a Keycloak token — aborting E2E run"
        exit 1
    fi
    if ! wait_for_gateway_routes; then
        echo -e "  ${FAIL} Gateway routing not ready — aborting E2E run"
        exit 1
    fi
    echo ""

    # ---- Phase 4: Run the E2E suite ------------------------------------------
    echo "── Phase 4: Running REST Assured E2E suite (e2e-tests module) ───────"
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