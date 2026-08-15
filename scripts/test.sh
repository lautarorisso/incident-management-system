#!/usr/bin/env bash
# =============================================================================
# Incident Management System — Test Runner
# =============================================================================
# Runs the full Maven test suite with the environment required by the
# embedded MongoDB (de.flapdoodle) used in notification-service tests.
#
# On distributions without OpenSSL 1.1 (e.g. Arch/CachyOS with OpenSSL 3.x),
# the mongod binary flapdoodle downloads is the Ubuntu 20.04 build, which
# links against libssl.so.1.1 / libcrypto.so.1.1. Those libs live in
# ~/.local/share/openssl-1.1 (extracted from the Ubuntu focal package).
# This script exports LD_LIBRARY_PATH only when that directory exists, so it
# is a no-op on systems that already provide OpenSSL 1.1.
#
# Usage:
#   ./scripts/test.sh                        # full build + tests (same as ./mvnw test)
#   ./scripts/test.sh -pl services/notification-service
# =============================================================================

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OPENSSL_11_DIR="${OPENSSL_11_DIR:-$HOME/.local/share/openssl-1.1}"

if [ -d "$OPENSSL_11_DIR" ]; then
    export LD_LIBRARY_PATH="$OPENSSL_11_DIR${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
fi

cd "$ROOT_DIR"
if [ "$#" -eq 0 ]; then
    set -- test
fi
exec ./mvnw "$@"
