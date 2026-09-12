# Archive Report: security-token-relay

## Metadata

| Field | Value |
|-------|-------|
| **Change** | security-token-relay |
| **Date** | 2026-09-12 |
| **Artifact Store** | openspec |
| **Mode** | openspec |
| **Verification** | PASS (256/256 tests, E2E 15/15, 41/45 scenarios COMPLIANT, CRITICAL 0, WARNING 0) |
| **Task Completion** | 26/26 tasks [x] |
| **Evidence Hash** | sha256:917b4ec84032973686fac4943a54e096c507f149d332a119f3b7d27bb7c1 |
| **Commits** | 9be3661, 772f8d1, 3db3b80, e7b5ee2, e076d72 (5 work units) |
| **Submodule** | config-server @ 2a822a4f92236b3da1a005d13f56b5ae45424521 |

## Source of Truth Updated

The following main specs now reflect the new behavior (created from delta specs):

| Domain | Action | Details |
|--------|--------|---------|
| `feign-token-relay` | Created | Authorization header forwarding via Feign `RequestInterceptor`; thread-local safety; interceptor scope |
| `gateway-cleanup` | Created | `/eureka/**` removed from public routes; `UserIdHeaderFilter` deleted; `X-User-Id` consumption eliminated; public routes limited to actuator/docs |
| `jwt-resource-server` | Created | Local JWT validation in 3 services; shared `SecurityFilterChain` factory; issuer-uri from Config Server; public endpoint exemptions |
| `notification-owner-check` | Created | `sub == userId` or `ROLE_ADMIN` authorization on `GET /api/notifications`; 403 exacto; role matrix separation |
| `role-mapping` | Created | `ims-admin` → `ROLE_ADMIN`, `ims-agent` → `ROLE_AGENT`, `ims-user` → `ROLE_USER`; servlet converter port from gateway reactive |

## Specs Synced (5 domains → `openspec/specs/`)

Each delta spec was a full spec (no pre-existing main specs in `openspec/specs/`). All 5 domains copied mechanically via shell `cp -R` + `diff -r` verification.

## Tasks Verified

- **Total**: 26 tasks
- **Complete**: 26/26 (all `[x]`)
- **Phases**: 7 (Foundation → Core → Integration → Gateway Cleanup → Config Server → Testing → Cleanup)
- **Unchecked**: 0

## Verification Summary

- **Tests**: 256 passed, 0 failed, 0 skipped
  - shared: 18, user-service: 17, notification: 56, incident: 122, gateway: 43
- **E2E**: 15/15 against real stack (GatewaySecurityE2E role matrix + IncidentFlowE2E owner-check 403)
- **Spec Compliance**: 41/45 scenarios COMPLIANT; 4 PARTIAL (edge cases covered by framework defaults, not security gaps)
- **CRITICAL**: 0
- **WARNING**: 0
- **SUGGESTION**: 5 non-blocking (explicit tests for expired/invalid/wrong-issuer tokens, missing @Import, Agent accessing other's notifications, multi-role ADMIN, missing userId)

## Archive Contents

- proposal.md ✅
- specs/ (5 domains) ✅
- design.md ✅
- tasks.md ✅ (26/26 complete)
- verify-report.md ✅ (PASS)
- archive-report.md ✅ (this file)
- exploration.md ✅

## Mechanical Verification

- `diff -r` (source snapshot → archive destination): **EMPTY** (no differences)
- Active changes directory: **no longer contains** `security-token-relay`
- Git mv applied to tracked files; verified byte-identity

## Notes

- No main specs existed prior to this archive (fresh `openspec/specs/`), so all 5 delta specs became full main specs.
- The `verify-report.md` was at the project root (untracked) and moved into the archive alongside other artifacts.
- Config-server submodule was already committed at `2a822a4` with `api-gateway.yaml` without `/eureka/web`.
- No CRITICAL or WARNING issues; archive proceeds per gentle-ai policy.
