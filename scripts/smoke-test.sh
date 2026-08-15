#!/usr/bin/env bash
# =============================================================================
# Incident Management System — Smoke Test
# =============================================================================
# End-to-end smoke test: health checks all services, creates an incident,
# assigns it to a real user, transitions it, and verifies the notification
# pipeline (outbox → RabbitMQ → notification-service) delivers a notification.
#
# Usage:
#   ./scripts/smoke-test.sh              # default (local ports)
#   GATEWAY_URL=http://localhost:8080 ./scripts/smoke-test.sh
#
# Prerequisites:
#   curl, jq (recommended)
#   All services running (or `docker-compose up -d --build`)
# =============================================================================

set -euo pipefail

# ---- Configuration ----------------------------------------------------------
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
INCIDENT_URL="${INCIDENT_URL:-http://localhost:8081}"
NOTIFICATION_URL="${NOTIFICATION_URL:-http://localhost:8083}"
USER_URL="${USER_URL:-http://localhost:8082}"
DISCOVERY_URL="${DISCOVERY_URL:-http://localhost:8761}"

TIMEOUT_SEC="${TIMEOUT_SEC:-120}"
POLL_INTERVAL="${POLL_INTERVAL:-5}"

# ---- Colors -----------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color
PASS="${GREEN}✓${NC}"
FAIL="${RED}✗${NC}"
SKIP="${YELLOW}−${NC}"

# ---- Counters ---------------------------------------------------------------
PASSED=0
FAILED=0
SKIPPED=0
HAS_JQ=false

# ---- Helpers ----------------------------------------------------------------
pass()   { echo -e "  ${PASS} $1"; PASSED=$((PASSED + 1)); }
fail()   { echo -e "  ${FAIL} $1"; FAILED=$((FAILED + 1)); }
skip()   { echo -e "  ${SKIP} $1"; SKIPPED=$((SKIPPED + 1)); }
info()   { echo -e "  ${YELLOW}→${NC} $1"; }

# Extracts the last UUID in a JSON document (no jq fallback).
# With jq prefer `jq -r '.[0].id'` instead; this is only for degraded mode.
extract_uuid() {
    sed -n 's/.*"\([0-9a-f]\{8\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{4\}-[0-9a-f]\{12\}\)".*/\1/p' | head -1
}

check_jq() {
    if ! command -v jq &>/dev/null; then
        echo "WARNING: jq not found — JSON response parsing disabled"
        return 1
    fi
    return 0
}

check_curl() {
    if ! command -v curl &>/dev/null; then
        echo "ERROR: curl is required but not installed"
        exit 1
    fi
}

wait_for_service() {
    local name="$1"
    local url="$2"
    local max_attempts=$((TIMEOUT_SEC / POLL_INTERVAL))
    local attempt=0

    info "Waiting for ${name} (${url}) ..."
    while [ $attempt -lt "$max_attempts" ]; do
        if curl -sf "${url}/actuator/health" &>/dev/null; then
            pass "${name} is healthy"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep "$POLL_INTERVAL"
    done
    fail "${name} not healthy after ${TIMEOUT_SEC}s"
    return 1
}

# Polls the notifications endpoint until at least one notification exists
# for the given user. Proves the full pipeline: incident event → outbox →
# RabbitMQ → notification-service.
wait_for_notification() {
    local user_id="$1"
    local max_attempts=$((TIMEOUT_SEC / POLL_INTERVAL))
    local attempt=0

    info "Polling notifications for user ${user_id} ..."
    while [ $attempt -lt "$max_attempts" ]; do
        local response
        response=$(curl -sf "${GATEWAY_URL}/api/notifications?userId=${user_id}" 2>&1) || response=""

        if [ -n "$response" ]; then
            if [ "$HAS_JQ" = true ]; then
                local count
                count=$(echo "$response" | jq '. | length')
                if [ "${count:-0}" -gt 0 ]; then
                    pass "Notification pipeline OK — ${count} notification(s) for user ${user_id}"
                    return 0
                fi
            elif [ -n "$(echo "$response" | extract_uuid)" ]; then
                pass "Notification pipeline OK — notification received for user ${user_id}"
                return 0
            fi
        fi

        attempt=$((attempt + 1))
        sleep "$POLL_INTERVAL"
    done
    fail "No notification received for user ${user_id} within ${TIMEOUT_SEC}s"
    return 1
}

# ---- Main -------------------------------------------------------------------
main() {
    check_jq && HAS_JQ=true
    check_curl

    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  IMS Smoke Test"
    echo "  $(date '+%Y-%m-%d %H:%M:%S')"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""

    # ---- Phase 1: Health Checks --------------------------------------------
    echo "── Phase 1: Health Checks ──────────────────────────────────────────"

    wait_for_service "Discovery Service" "${DISCOVERY_URL}"
    wait_for_service "API Gateway"        "${GATEWAY_URL}"
    wait_for_service "Incident Service"   "${INCIDENT_URL}"
    wait_for_service "Notification Service" "${NOTIFICATION_URL}"
    wait_for_service "User Service"       "${USER_URL}"

    # ---- Phase 2: Create Incident ------------------------------------------
    echo ""
    echo "── Phase 2: Create Incident ────────────────────────────────────────"

    local incident_id
    local create_payload='{
        "title": "Smoke Test Incident",
        "description": "Created by smoke-test.sh",
        "priority": "HIGH"
    }'

    info "POST /api/incidents"
    local create_response
    create_response=$(curl -sf -X POST "${GATEWAY_URL}/api/incidents" \
        -H "Content-Type: application/json" \
        -d "$create_payload" 2>&1) || {
        fail "Create incident failed (API Gateway) — trying direct incident-service"
        create_response=$(curl -sf -X POST "${INCIDENT_URL}/api/incidents" \
            -H "Content-Type: application/json" \
            -d "$create_payload" 2>&1) || {
            fail "Create incident failed (direct)"
            return 1
        }
    }

    if [ "$HAS_JQ" = true ]; then
        incident_id=$(echo "$create_response" | jq -r '.id // empty')
    else
        incident_id=$(echo "$create_response" | extract_uuid)
    fi

    if [ -n "$incident_id" ]; then
        pass "Incident created: id=${incident_id}"
    else
        fail "Could not extract incident ID from response: ${create_response}"
        incident_id="smoke-test-fallback-$(date +%s)"
        skip "Continuing with synthetic ID: ${incident_id}"
    fi

    # ---- Phase 3: Retrieve Incident ----------------------------------------
    echo ""
    echo "── Phase 3: Retrieve Incident ──────────────────────────────────────"

    info "GET /api/incidents/${incident_id}"
    if curl -sf "${GATEWAY_URL}/api/incidents/${incident_id}" -o /dev/null 2>&1; then
        pass "Retrieved incident ${incident_id}"
    else
        if curl -sf "${INCIDENT_URL}/api/incidents/${incident_id}" -o /dev/null 2>&1; then
            pass "Retrieved incident ${incident_id} (direct)"
        else
            fail "Failed to retrieve incident ${incident_id}"
        fi
    fi

    # ---- Phase 4: List Incidents -------------------------------------------
    echo ""
    echo "── Phase 4: List Incidents ─────────────────────────────────────────"

    info "GET /api/incidents"
    if curl -sf "${GATEWAY_URL}/api/incidents" -o /dev/null 2>&1; then
        pass "Listed incidents"
    else
        if curl -sf "${INCIDENT_URL}/api/incidents" -o /dev/null 2>&1; then
            pass "Listed incidents (direct)"
        else
            fail "Failed to list incidents"
        fi
    fi

    # ---- Phase 5: Real Notification Pipeline -------------------------------
    echo ""
    echo "── Phase 5: Verify Notification Pipeline ───────────────────────────"

    info "GET /api/users"
    local users_response
    users_response=$(curl -sf "${GATEWAY_URL}/api/users" 2>&1) || {
        users_response=$(curl -sf "${USER_URL}/api/users" 2>&1) || {
            users_response=""
        }
    }

    local assignee_id=""
    if [ -n "$users_response" ]; then
        if [ "$HAS_JQ" = true ]; then
            assignee_id=$(echo "$users_response" | jq -r '.[0].id // empty' 2>/dev/null)
        else
            assignee_id=$(echo "$users_response" | extract_uuid)
        fi
    fi

    if [ -z "$assignee_id" ]; then
        skip "No users available in user-service — cannot validate notification pipeline"
    else
        pass "Resolved assignee user ${assignee_id}"

        info "PUT /api/incidents/${incident_id}/assign"
        if curl -sf -X PUT "${GATEWAY_URL}/api/incidents/${incident_id}/assign" \
                -H "Content-Type: application/json" \
                -d "{\"assigneeId\":\"${assignee_id}\"}" -o /dev/null 2>&1; then
            pass "Incident assigned to ${assignee_id}"
        else
            if curl -sf -X PUT "${INCIDENT_URL}/api/incidents/${incident_id}/assign" \
                    -H "Content-Type: application/json" \
                    -d "{\"assigneeId\":\"${assignee_id}\"}" -o /dev/null 2>&1; then
                pass "Incident assigned to ${assignee_id} (direct)"
            else
                fail "Failed to assign incident ${incident_id}"
            fi
        fi

        info "PUT /api/incidents/${incident_id}/transition → IN_PROGRESS"
        if curl -sf -X PUT "${GATEWAY_URL}/api/incidents/${incident_id}/transition" \
                -H "Content-Type: application/json" \
                -d '{"newStatus":"IN_PROGRESS"}' -o /dev/null 2>&1; then
            pass "Incident transitioned to IN_PROGRESS"
        else
            if curl -sf -X PUT "${INCIDENT_URL}/api/incidents/${incident_id}/transition" \
                    -H "Content-Type: application/json" \
                    -d '{"newStatus":"IN_PROGRESS"}' -o /dev/null 2>&1; then
                pass "Incident transitioned to IN_PROGRESS (direct)"
            else
                fail "Failed to transition incident ${incident_id}"
            fi
        fi

        wait_for_notification "$assignee_id"
    fi

    # ---- Phase 6: User Service Listing ------------------------------------
    echo ""
    echo "── Phase 6: User Service Listing ───────────────────────────────────"

    info "GET /api/users"
    if curl -sf "${GATEWAY_URL}/api/users" -o /dev/null 2>&1; then
        pass "User service listing available"
    else
        if curl -sf "${USER_URL}/api/users" -o /dev/null 2>&1; then
            pass "User service listing available (direct)"
        else
            skip "User listing endpoint unavailable"
        fi
    fi

    # ---- Summary ------------------------------------------------------------
    local total=$((PASSED + FAILED + SKIPPED))
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    echo "  Results: ${PASSED} passed, ${FAILED} failed, ${SKIPPED} skipped (${total} total)"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""

    if [ "$FAILED" -gt 0 ]; then
        exit 1
    fi
}

main "$@"
