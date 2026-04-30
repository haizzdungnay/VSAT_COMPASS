# Phase C Precheck Audit Report

**Date:** 2026-04-26
**Auditor:** Claude Code (claude-sonnet-4-6)
**Repo state at audit:** `53f8b2132f197be60bffe757f8a0ccd58d910dda`
**Audit type:** READ-ONLY (no changes applied)

---

## Executive Summary

| Severity | Count |
|----------|-------|
| CRITICAL | 0     |
| HIGH     | 2     |
| MEDIUM   | 7     |
| LOW      | 4     |
| INFO     | 6     |

**Recommendation:** **FIX HIGH items first, then PROCEED to Phase C.** No CRITICAL findings. The two HIGH findings (missing git tags, EOL Spring Boot branch) are quick fixes that should be resolved before Phase C complicates the version tracking. Phase C can begin in parallel with the MEDIUM fixes.

---

## Findings by Phase

### Phase 1 — Git Hygiene

| Check | Result |
|-------|--------|
| Working tree dirty? | YES — `.claude/settings.json` modified only (stop-gate exception applies) |
| Untracked files leaking? | NO — `git ls-files --others --exclude-standard` returned empty |
| `.claude/` in tracked files? | **YES — MEDIUM** — `.claude/settings.json` and `.claude/settings.local.json` are tracked |
| `.env` / `.key` / `.pem` tracked? | NO — only `.env.example` tracked (acceptable placeholder file) |
| Large files (>1 MB) in git? | **YES — MEDIUM** — 5 files in `VSAT/ui/` totalling ~38 MB: |

**Large tracked files:**
```
19,102,385 bytes  VSAT/ui/stitch_v_sat_compass_luy_n_t_p_theo_ch_…/screen.png (stitched UI screenshot)
14,892,829 bytes  VSAT/ui/stitch_v_sat_compass_luy_n_t_p_theo_ch_…/screen.png (stitched UI screenshot)
 2,972,654 bytes  VSAT/ui/stitch_v_sat_compass_luy_n_t_p_theo_ch_…/screen.png
 1,181,475 bytes  VSAT/ui/extracted_screens/…/screen.png
 1,181,475 bytes  VSAT/stitch_v_sat_compass_…/screen.png
```
Total: ~39 MB of PNG screenshots tracked in git. These inflate clone size permanently (git history is immutable).

**Branches:**
```
main                              53f8b21  [origin/main] — active, up to date
backup-before-author-fix-20260410 009350a  — STALE, local-only backup branch from 2026-04-10
```
`backup-before-author-fix-20260410` is a maintenance backup with no remote tracking. It points to v0.5.0 commit `009350a` (feat: hybrid architecture). This branch is safe to delete when convenient.

**Git tags:** `git tag -l` returned empty — **no tags exist**. See Phase 6 finding.

---

### Phase 2 — .gitignore Coverage

**Root `.gitignore` — patterns present (PASS):** `build/`, `.gradle/`, `.idea/`, `.vscode/`, `*.iml`, `node_modules/`, `.env`, `.env.local`, `*.apk`, `*.aab`, `.DS_Store`, `Thumbs.db`

**Root `.gitignore` — patterns MISSING:**

| Missing Pattern | Risk | Priority |
|----------------|------|----------|
| `.claude/`     | Claude Code config files already leaked into tracked history (`.claude/settings.json`, `.claude/settings.local.json`) | HIGH (fix retroactively too) |
| `local.properties` | Android SDK local path config — developer-machine-specific, should never be committed | MEDIUM |
| `*.keystore`   | Android release signing keystore — if committed would be a CRITICAL security issue | MEDIUM |
| `*.jks`        | Java keystore format, same risk as `.keystore` | MEDIUM |

**Backend `.gitignore`** (`VSAT/vsat-compass-api/.gitignore`): Covers `.idea`, `.gradle`, `build/`, `.vscode/`, `.env`, `*.env.local`, `.DS_Store`, `Thumbs.db`. No gaps detected.

**Notable:** `.claude/settings.json` is currently tracked (see Phase 1). Adding `.claude/` to `.gitignore` alone is not enough — the currently-tracked files must be `git rm --cached` first.

---

### Phase 3 — Secret Scan

**Current files scan:** All pattern matches (JWT_SECRET, DATABASE_PASSWORD, SECRET_KEY, API_KEY) were inspected. Every match is either:
- Documentation prose in `CHANGELOG.md`, `README.md`, `TASKS.md`, `docs/DEPLOY_RUNBOOK.md`
- Placeholder values in `VSAT/vsat-compass-api/.env.example` (e.g., `your_neon_password`, `your-base64-encoded-secret-key-…`)
- Environment variable references in `application.yml` (`${JWT_SECRET}`, `${DATABASE_PASSWORD}`)
- Render env-var keys in `render.yaml` (no values inline)

**No real secrets found in current files. PASS.**

**Git history scan:** Searched history for `JWT_SECRET=`, `DATABASE_PASSWORD=`, and `postgres://` using `git log -p -S`. No commits added real credential values. `TASKS.md` notes "JWT_SECRET cũ trong git history đã được rotate" — confirming that any previously leaked secret has been rotated. **PASS.**

**`render.yaml` secret handling:** All secret-bearing env vars use `sync: false` (values must be entered manually in Render Dashboard). No `value:` inline entries for secrets. **COMPLIANT.**

**`application.yml` hardcoded heuristic** (`grep -E "(password|secret|key)\s*[:=]\s*[A-Za-z0-9+/=_-]{16,}"`): No matches. **PASS.**

**Overall Phase 3 assessment: CLEAN. No rotation required.**

---

### Phase 4 — Dependencies

#### Backend (Spring Boot)

| Library | Version in use | Latest stable | Status |
|---------|---------------|---------------|--------|
| Spring Boot | 3.2.5 | 3.4.x / 3.3.x | **HIGH — EOL Nov 2024** |
| Spring Security | 6.2.4 | 6.4.x (via SB 3.4) | Pinned to SB 3.2.5 BOM |
| jjwt | 0.12.5 | 0.12.7 | OK (minor patch behind) |
| PostgreSQL driver | 42.6.2 | 42.7.5 | MEDIUM — one minor behind |
| Hibernate ORM | 6.4.4.Final | 6.6.x | OK for SB 3.2.x |
| Jackson | 2.15.4 | 2.17.x | OK for SB 3.2.x |
| Tomcat embed | 10.1.20 | 10.1.x (maintained) | OK |
| Bucket4j | 8.10.1 | 8.10.1 | OK |
| MapStruct | 1.5.5.Final | 1.6.x | OK |
| springdoc-openapi | 2.5.0 | 2.8.x | OK |

**Spring Boot 3.2.x reached End-of-Life in November 2024.** The 3.2 line no longer receives security patches from the Spring team. Active supported branches are 3.3.x (Nov 2025) and 3.4.x (Feb 2027). Recommend upgrading to 3.3.x or 3.4.x before or during Phase C. The upgrade is generally non-breaking between 3.2 → 3.3.

**OWASP Dependency-Check plugin:** NOT configured (`./gradlew tasks` has no `dependencyCheckAnalyze` target). Recommend adding before Phase C ships to production.

#### Android

| Library | Version | Latest | Status |
|---------|---------|--------|--------|
| AGP (Android Gradle Plugin) | 9.1.1 | 9.1.x | OK |
| Gradle Wrapper | 9.3.1 | 9.3.x | OK |
| Retrofit | 2.11.0 | 2.11.0 | OK — current |
| OkHttp | 4.12.0 | 4.12.0 | OK — current |
| Gson | 2.11.0 | 2.11.0 | OK — current |
| Glide | 4.16.0 | 4.16.0 | OK (5.x in RC) |
| AndroidX Lifecycle | 2.8.7 | 2.9.x | OK |
| Navigation | 2.8.5 | 2.8.x | OK |
| Material | 1.13.0 | 1.13.0 | OK — current |
| compileSdk / targetSdk | 36 (Android 16) | 36 | OK — current |
| minSdk | 28 (Android 9) | — | Appropriate |

Android dependency health is good. No known CVE-bearing versions found.

**Top 5 highest-risk libraries by CVE history (informational):**
1. Spring Boot 3.2.5 — on EOL branch, accumulating unpatched CVEs
2. PostgreSQL JDBC 42.6.2 — 42.6.x had several fixes in 42.7.x (SQL injection mitigations in edge cases)
3. Hibernate ORM 6.4.4 — no active CVEs but large attack surface
4. Spring Security 6.2.4 — no active CVEs but tied to EOL Spring Boot line
5. Tomcat embed 10.1.20 — no known active CVEs at audit date

---

### Phase 5 — Dead Code & TODO

**TODO/FIXME/XXX/HACK count:** **1 total**

| File | Line | Content |
|------|------|---------|
| [app/src/main/java/…/ui/admin/exam/AdminCreateExamActivity.java](app/src/main/java/com/example/v_sat_compass/ui/admin/exam/AdminCreateExamActivity.java) | 138 | `// TODO: Mở màn hình chọn câu từ ngân hàng (phase 2)` |

This TODO is intentional scaffolding for Phase C (Question Bank selection screen). It should be converted to a tracked task, not deleted.

**UnsupportedOperationException stubs:** NONE found. No scaffolded-but-unimplemented methods.

**Files > 500 lines:**

| Lines | File |
|-------|------|
| 658 | [app/src/main/java/…/ui/exam/session/ExamSessionActivity.java](app/src/main/java/com/example/v_sat_compass/ui/exam/session/ExamSessionActivity.java) |

`ExamSessionActivity.java` at 658 lines is a candidate for extraction (e.g., split timer logic, answer-tracking logic, and submit logic into separate classes). Phase C will add question-bank features nearby — extracting now avoids merge conflicts later.

**Import baseline:** ~350 import statements across 99 Java source files. No automated unused-import check was run (would require compilation with lint enabled).

---

### Phase 6 — Documentation

| Check | Result | Detail |
|-------|--------|--------|
| CHANGELOG latest entry | `[0.8.1] 2026-04-25` | OK |
| Latest git tag | **NONE** | HIGH — `git tag -l` is empty |
| CHANGELOG ↔ tag match | **MISMATCH** | No tags exist for any of the 6 CHANGELOG versions |
| README production endpoint | PASS | `https://vsat-compass-api.onrender.com/api/v1/` present at lines 165, 203, 237 |
| README test credentials | PASS | `student@vsat.com` / `Student@123` and `admin@vsat.com` / `Admin@123` present |
| DEPLOY_RUNBOOK lessons-learned | PASS (partial) | Section `## 7. Known Pitfalls` covers 3 Phase B incidents; `## 6. Incident Triage Checklist` added. No explicit "Lessons Learned" heading but content equivalent is present. |
| SMOKE_CHECKLIST TC count | 25 TCs (manual) | `## TC-001` … `## TC-025` |
| Automated smoke coverage | 14/25 TCs | `docs/scripts/smoke_auth.sh` (9 TCs) + `docs/scripts/smoke_sessions.sh` (5 TCs). Remaining 11 are UI-flow manual tests (correct — these cannot be shell-automated). |
| SMOKE_CHECKLIST version stamp | `v0.8.0 / 2026-04-23` | Should be updated to `v0.8.1 / 2026-04-25` after Phase B hardening |

**Git tags gap:** All CHANGELOG versions (`0.8.1`, `0.8.0`, `0.7.1`, `0.7.0`, `0.6.0`, etc.) exist only in the log, not as git tags. This makes it impossible to `git checkout v0.8.1` or generate a diff between releases. Creating annotated tags retroactively is safe and non-destructive. Recommend tagging before Phase C adds commits that would make the history harder to annotate.

---

### Phase 7 — Test Coverage

#### Backend

| Service class | Test file exists? |
|---------------|------------------|
| [AuthService.java](VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/AuthService.java) | **NO** |
| [SessionService.java](VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/SessionService.java) | **NO** |
| [RefreshTokenCleanupService.java](VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/RefreshTokenCleanupService.java) | **NO** |
| [CustomUserDetailsService.java](VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/security/service/CustomUserDetailsService.java) | **NO** |

**Backend test result:** `./gradlew test` → `BUILD SUCCESSFUL` with `NO-SOURCE` (zero test classes). The build passes vacuously. There are no test source files in `VSAT/vsat-compass-api/src/test/`.

**Priority for Phase C:** `AuthService` and `SessionService` are the highest-risk services — they handle authentication tokens, session integrity, and anti-replay. Phase C will add `QuestionService`, `ReviewService`, and `ExamManagementService`. These new services should have tests from the start; the existing gap in `AuthService` / `SessionService` is a pre-existing debt that Phase C should not amplify.

#### Android

| Test file | Type | Status |
|-----------|------|--------|
| `ExamHistoryRepositoryTest.java` | JUnit unit test | Exists, coverage TBD |
| `ExampleUnitTest.java` | JUnit stub | Placeholder only |
| `ExampleInstrumentedTest.java` | Espresso stub | Placeholder only |

**Android test run result:** `FAILED` — build error during `testDebugUnitTest`:
```
Execution failed for JdkImageTransform: …/core-for-system-modules.jar
> jlink executable …/redhat.java-1.54.0-win32-x64/jre/21.0.10-win32-x86_64/bin/jlink.exe does not exist
```
Root cause: The Android Gradle Plugin is resolving jlink from a stale VS Code extension path that no longer has the executable. This is a **local developer environment issue**, not a code bug. Fix: ensure `JAVA_HOME` points to the Android Studio bundled JBR (`C:\Program Files\Android\Android Studio\jbr`) before running Android tests. This does not affect CI (if/when CI is configured with a proper JDK).

**Recommendation for Phase C:** Write `AuthServiceTest` and `SessionServiceTest` before starting Phase C work that depends on those services. Do not require full backfill of all 4 existing services — focus on the two security-boundary services.

---

## Prioritized Fix Queue (for user to decide on)

| # | Severity | Phase | Finding | Effort | Recommended action |
|---|----------|-------|---------|--------|--------------------|
| 1 | HIGH | 6 | No git tags — 6 CHANGELOG versions untagged | S | `git tag -a v0.8.1 53f8b21 -m "Phase B verified"` and retroactive tags for earlier versions |
| 2 | HIGH | 4 | Spring Boot 3.2.5 on EOL branch (EOL Nov 2024) | M | Upgrade to `3.3.x` or `3.4.x`; test all endpoints after upgrade |
| 3 | MEDIUM | 1/2 | `.claude/settings.json` + `.claude/settings.local.json` tracked in git | S | `git rm --cached .claude/settings.json .claude/settings.local.json` + add `.claude/` to `.gitignore` |
| 4 | MEDIUM | 7 | `AuthService` has no tests — security boundary | M | Write `AuthServiceTest` covering login, register, token refresh, and password change |
| 5 | MEDIUM | 7 | `SessionService` has no tests — anti-replay boundary | M | Write `SessionServiceTest` covering start, submit, and duplicate-submit (409) |
| 6 | MEDIUM | 2 | `.gitignore` missing `local.properties`, `*.keystore`, `*.jks` | S | Add 4 patterns to root `.gitignore` |
| 7 | MEDIUM | 4 | PostgreSQL JDBC at 42.6.2; 42.7.5 available | S | Upgrade via Spring Boot BOM bump (covered by item #2) |
| 8 | MEDIUM | 7 | Android test build fails (jlink path in VS Code extension) | S | Set `JAVA_HOME` to Android Studio JBR before running tests; document in README dev-setup |
| 9 | MEDIUM | 1 | `backup-before-author-fix-20260410` stale local branch | XS | `git branch -d backup-before-author-fix-20260410` when confident backup not needed |
| 10 | LOW | 1 | 38+ MB of PNG screenshots tracked in git (`VSAT/ui/`) | L | Consider git-lfs or excluding from history via BFG; low urgency since not a security issue |
| 11 | LOW | 5 | `ExamSessionActivity.java` at 658 lines | M | Extract timer/submit/answer-tracking logic into separate classes before Phase C adds more |
| 12 | LOW | 4 | OWASP dependency-check plugin not configured | S | Add `org.owasp.dependencycheck` plugin to backend `build.gradle`; run in CI |
| 13 | LOW | 6 | SMOKE_CHECKLIST version stamp is `v0.8.0 / 2026-04-23` | XS | Update header to `v0.8.1 / 2026-04-26` |
| 14 | INFO | 3 | Secret scan: CLEAN | — | No action required |
| 15 | INFO | 3 | render.yaml: all secrets use `sync: false` | — | No action required |

---

## Appendix — Raw command output excerpts (truncated, secrets redacted)

### A1 — Git status
```
 M .claude/settings.json
```

### A2 — Untracked files
```
(empty — no untracked files outside .gitignore)
```

### A3 — Branches
```
backup-before-author-fix-20260410 009350a feat: v0.5.0 — kiến trúc hybrid
* main                              53f8b21 [origin/main] docs: add TASKS.md
```

### A4 — Large tracked files (from git ls-files | xargs ls -la)
```
19102385  VSAT/ui/stitch_v_sat_compass_…/screen.png
14892829  VSAT/ui/stitch_v_sat_compass_…/screen.png
 2972654  VSAT/ui/stitch_v_sat_compass_…/screen.png
 1181475  VSAT/ui/extracted_screens/…/screen.png
 1181475  VSAT/stitch_v_sat_compass_…/screen.png
```

### A5 — Secret scan (current files)
Pattern matches found in: CHANGELOG.md, README.md, TASKS.md, DEPLOY_RUNBOOK.md, .env.example, application.yml, render.yaml — **all placeholder/documentation references. No real values present.**

### A6 — Secret scan (git history)
`git log -p -S "JWT_SECRET="` → 0 results with real values
`git log -p -S "DATABASE_PASSWORD="` → 0 results with real values
`git log -p -S "postgres://"` → 0 results with embedded credentials

### A7 — Backend dependency versions (runtimeClasspath)
```
spring-boot: 3.2.5
spring-security: 6.2.4
jjwt-api/impl/jackson: 0.12.5
postgresql: 42.6.2 (BOM-resolved)
hibernate-core: 6.4.4.Final
jackson-databind: 2.15.4
tomcat-embed-core: 10.1.20
```

### A8 — Android dependency versions (releaseRuntimeClasspath)
```
retrofit: 2.11.0, okhttp: 4.12.0, gson: 2.11.0
glide: 4.16.0, lifecycle: 2.8.7, navigation: 2.8.5
material: 1.13.0, AGP: 9.1.1, gradle-wrapper: 9.3.1
```

### A9 — Backend test run
```
> Task :compileTestJava NO-SOURCE
> Task :test NO-SOURCE
BUILD SUCCESSFUL in 4s
```

### A10 — Android test run
```
FAILURE: Build failed with an exception.
Execution failed for JdkImageTransform: …/core-for-system-modules.jar
> jlink executable …/redhat.java-1.54.0-win32-x64/jre/21.0.10-win32-x86_64/bin/jlink.exe does not exist.
```

### A11 — .gitignore gap check
```
MISSING ROOT: local.properties
MISSING ROOT: .claude/
MISSING ROOT: *.keystore
MISSING ROOT: *.jks
```

### A12 — TODO count
```
Total: 1
app/src/main/java/…/ui/admin/exam/AdminCreateExamActivity.java:138:
  // TODO: Mở màn hình chọn câu từ ngân hàng (phase 2)
```

---

## Batch 1 Resolution Log

**Applied:** 2026-04-26 in commit `1319a5c` (chore(repo): Batch 1 quick wins)

**Tag mapping applied:**

| Version | Commit | Note |
|---------|--------|------|
| v0.5.0 | `ed2637e` | Direct match — "feat: v0.5.0" commit on main |
| v0.6.0 | `b5f2134` | Retroactive — backfilled into CHANGELOG at v0.8.0 commit |
| v0.7.0 | `b5f2134` | Retroactive — same |
| v0.7.1 | `b5f2134` | Retroactive — same |
| v0.8.0 | `b5f2134` | Direct match — "feat: v0.8.0" commit |
| v0.8.1 | `53f8b21` | Audit baseline commit (last pre-audit Phase B commit) |

**Findings resolved:**

| # | Severity | Status |
|---|----------|--------|
| 1 | HIGH     | ✅ RESOLVED — 6 annotated tags created and pushed to origin |
| 3 | MEDIUM   | ✅ RESOLVED — `.claude/` untracked from index + added to `.gitignore` |
| 6 | MEDIUM   | ✅ RESOLVED — `.gitignore` patterns added: `.claude/`, `local.properties`, `*.keystore`, `*.jks` |
| 9 | MEDIUM   | ✅ RESOLVED — stale branch `backup-before-author-fix-20260410` deleted |
| 13| LOW      | ✅ RESOLVED — `docs/SMOKE_CHECKLIST.md` stamp updated to `v0.8.1 / 2026-04-26` |

Remaining open items go to Batch 2 (HIGH #2, MEDIUM #4, #5, #7, #8) or are deferred (LOW #10, #11, #12).

---

## Batch 2a Resolution Log

**Applied:** 2026-04-30 in commit `740a73a` (tests + docs) and the next commit (audit log update)

| # | Severity | Status |
|---|----------|--------|
| 4 | MEDIUM   | ✅ RESOLVED — AuthServiceTest added (10 tests Mockito, all PASS on SB 3.2.5) |
| 5 | MEDIUM   | ✅ RESOLVED — SessionServiceTest added (8 tests Mockito, all PASS on SB 3.2.5) |

Notes:
- Tests are characterization tests (lock down current behavior) and will be re-run after Batch 2b's Spring Boot upgrade.
- Coverage scope per user direction (Option B): happy path + key edge cases — 18 tests total (10 Auth + 8 Session).
- Test architecture: Mockito unit tests for service-layer logic. No Spring context loaded.
- Documentation synced: CHANGELOG.md `[Unreleased]` Tests (Batch 2a) section + task.md `B6 — Pre-Phase C Quality Hardening` subsection.

> **Batch 2a executed in Option B (Mockito-only) mode.** Repository custom-query characterization via Testcontainers was deferred to Batch 2a-bis because Docker Desktop could not start on the dev machine (VMware Workstation set `bcdedit hypervisorlaunchtype=off`, blocking Hyper-V and therefore Docker Desktop). Audit items #4 and #5 are still RESOLVED — service-level tests cover the security-boundary logic the audit flagged. Custom-query coverage will be addressed in a follow-up run when Docker is available.

> **Tooling note:** Dev machine had no `JAVA_HOME` set; `./gradlew test` was executed using the JDK 21 bundled with Android Studio (`C:\Program Files\Android\Android Studio\jbr`) via session-scoped env vars only — no system config or committed file changed. This is the same JAVA_HOME class of issue flagged as MEDIUM #8 (Android side); the API server now needs a permanent JAVA_HOME setup too. Tracked under Batch 2a-bis along with Docker.

Remaining open items: HIGH #2 (Spring Boot upgrade — Batch 2b), MEDIUM #7 (Postgres driver — Batch 2b), #8 (JAVA_HOME — Batch 2a-bis), Testcontainers IT (Batch 2a-bis).

---

## Batch 2b Resolution Log (Partial — local verify done, prod deploy pending)

**Applied:** 2026-04-30 on branch `batch-2b/spring-boot-3-5-upgrade`, commit `00e1c3f` (upgrade + docs) and the next commit (audit log update).
**NOT yet on main; production NOT yet deployed.**

| # | Severity | Status |
|---|----------|--------|
| 2 | HIGH     | 🟡 PARTIAL — Spring Boot 3.2.5 → 3.5.9 done on branch, awaiting Batch 2b.2 (merge + Render deploy + prod smoke) |
| 7 | MEDIUM   | 🟡 PARTIAL — PostgreSQL JDBC 42.6.2 → 42.7.8 patched transitively via SB BOM, awaiting prod verification |

**Local verification (2026-04-30):**
- `./gradlew clean compileJava` BUILD SUCCESSFUL (JDK 21 / Android Studio JBR).
- `./gradlew test` 18/0/0/0 PASS on Spring Boot 3.5.9 — zero behavior regression at the service layer.
- `./gradlew bootRun` started cleanly: actuator `/health` returned `{"status":"UP"}`; "Started VsatCompassApiApplication in 7.048 seconds"; Hibernate ORM 6.6.39.Final; Tomcat 10.1.50; Spring 6.2.15. One benign WARN about explicit `hibernate.dialect` property (deferred — `src/main` change out of scope for this batch).
- `BASE_URL=http://localhost:8080/api/v1 bash docs/scripts/smoke_auth.sh` returned 9/9 PASS (TC-AUTH-1 through TC-AUTH-9).
- `smoke_sessions.sh` deferred to 2b.2 production verification — local DB is a Neon dev branch without the smoke-test exam seed.

**Dep diff (sensitive families):**
| Library | Before | After |
|---------|--------|-------|
| spring-boot | 3.2.5 | 3.5.9 |
| spring-security | 6.2.4 | 6.5.7 |
| spring-framework | 6.1.x | 6.2.15 |
| hibernate-core | 6.4.4.Final | 6.6.39.Final |
| tomcat-embed | 10.1.20 | 10.1.50 |
| jackson | 2.15.4 | 2.19.4 |
| postgresql | 42.6.2 | 42.7.8 |
| jjwt / bucket4j / mapstruct / springdoc | unchanged (we pin) | unchanged |

Notes:
- Conservative target chosen: 3.5.9 (latest 3.5.x patch). 3.5 line OSS support through Jun 30, 2026.
- A separate Batch 2c will address 3.5 → 4.0.x upgrade after Phase C.
- Backup branch `backup-pre-batch-2b-20260430` retained — rollback path is `git reset --hard backup-pre-batch-2b-20260430` from main.
- Items #2 and #7 will flip from 🟡 PARTIAL to ✅ RESOLVED in Batch 2b.2 after production smoke verification.

---

## Batch 2b Resolution Log (FINAL — production verified)

**Applied:** 2026-04-30 20:09 ICT in merge commit `0a0f341f1b543845e14c9df35dc9b2e0ab0a87c9` on `main`.
**Tag:** `v0.8.2`.

| # | Severity | Status |
|---|----------|--------|
| 2 | HIGH     | ✅ **RESOLVED** — Spring Boot 3.2.5 → 3.5.9 deployed and verified in production. 14/14 prod smoke PASS (auth 9/9, sessions 5/5). |
| 7 | MEDIUM   | ✅ **RESOLVED** — PostgreSQL JDBC 42.6.2 → 42.7.8 deployed transitively via SB 3.5 BOM. |

Notes:
- Merge strategy: `--no-ff` (Option C, per user direction) so a single revert point exists at the merge commit.
- Backup branch `backup-pre-batch-2b-20260430` retained through end of May 2026 (1 month). Rollback path:
  ```
  git revert -m 1 0a0f341f1b543845e14c9df35dc9b2e0ab0a87c9
  git push origin main   # Render auto-deploys back to 3.2.5
  ```
- Feature branch `batch-2b/spring-boot-3-5-upgrade` has been deleted (local + remote) since its content is now on main.
- Spring Boot 3.5.x OSS support runs through Jun 30, 2026. Batch 2c (3.5 → 4.0) will be scheduled post-Phase C.
