# V-SAT COMPASS — CHANGELOG

## [Unreleased]

### APK Release Track -- APK-1.1 hotfix build.gradle.kts imports (2026-05-24)

- Hotfix: added `import java.util.Properties` at the top of `app/build.gradle.kts`.
- Replaced `java.util.Properties()` with `Properties()` and `java.io.FileInputStream(file)` with `file.inputStream()`.
- Resolved Kotlin DSL "Unresolved reference 'util' / 'io'" errors introduced by APK-1.
- Gradle sync + `./gradlew app:signingReport` now succeed.
- No version bump; no dependency change; no source code change beyond the build script.

### APK Release Track -- APK-1 release config foundation (2026-05-24)

- Configured release signing via signingConfigs.release reading from keystore.properties (gitignored).
- Set versionCode=1, versionName="0.1.0" for first APK release (android/v0.1.0).
- Enabled minify + shrinkResources for release build type.
- Augmented proguard-rules.pro with keep rules for Retrofit, Gson, Glide, OkHttp, and model classes.
- Added keystore.properties.example template and .gitignore entries for keystore.properties and /app/release/.
- Added docs/ANDROID_RELEASE.md release runbook covering keystore generation, build, sideload distribution.
- Backend remains frozen at v0.10.2; no backend changes.
- Actual release APK build + smoke verification deferred to APK-2.
- Rebased Android APK release numbering from previous versionName="1.0" to versionName="0.1.0" for independent APK semver.

### Phase C1.6 Closeout -- finalize student backend integration (2026-05-24)

- Phase C1.6 fully closed across C1.6-A (v0.10.1), C1.6-A.5 (v0.10.2),
  and C1.6-B (Android, 85c6762).
- Local sample_*.json packs retained as offline fallback only (soft retire).
- Hard deletion deferred until APK ship; USE_BACKEND_EXAM_CONTENT=true makes
  backend the primary content source.
- Backend frozen at v0.10.2; no new runtime tag for Android-only batch.

### Phase C1.6-B -- Android student backend integration (2026-05-23)

- Added Android session-content POJO mirrors for backend question content and answer-key responses.
- Added backend content API methods getSessionQuestionContent and getSessionAnswerKeys while preserving the legacy getSessionQuestion path.
- Added SessionContentRepository for C1.6-A student session content endpoints.
- Added USE_BACKEND_EXAM_CONTENT flag to make student exam flow backend-first by default.
- Wired Home, Exam list, Exam detail, Exam session, and Review flows toward backend-first content with local sample packs retained as offline fallback only.
- Extended Android session-start response model with orderedQuestionIds from backend v0.10.2.
- Added Android unit/contract coverage for the new session content API/repository paths.

### Phase C1.6-B Closeout -- tracker sync after Android backend integration (2026-05-23)

- Flipped C1.6-B status to Done in task.md; recorded merge commit ca4b6f2.
- No runtime change; backend remains frozen at v0.10.2.
- C1.6-C (retire sample_*.json) remains open as next planned batch.

## [0.10.2] - 2026-05-22 — Phase C1.6-A.5

### Phase C1.6-A.5 — Session start response includes ordered question IDs (2026-05-22)

- Extended `SessionResponse.SessionInfo` with `orderedQuestionIds: List<Long>` populated from the `ExamQuestion` table at session start.
- Empty exams return an empty list instead of null.
- Added Mockito coverage for ordered question ID population.
- Added TC-SESSION-13 to the session smoke script/checklist.
- Runtime release will be tagged `v0.10.2` after merge, Render deploy, and production smoke pass.

## [0.10.1] - 2026-05-22 — Phase C1.6-A

### Phase C1.6-A — Backend student exam content delivery (2026-05-22)

- Added `GET /sessions/{sessionId}/questions/{questionId}` for authenticated student in-session question delivery with answer keys stripped.
- Added `GET /sessions/{sessionId}/answer-keys` for post-submit answer key and explanation retrieval.
- Extended session smoke coverage with TC-SESSION-8 through TC-SESSION-12.
- Released as `v0.10.1` after production smoke verification.

## [0.10.0] - 2026-05-22 — Phase C1.5

### Phase C1.5-A — Admin question picker backend endpoint (2026-05-22)

- Added `GET /admin/questions/picker` with filters (`status`, `subjectId`, `topicId`, `questionType`, `q`) and pagination.
- Default status filter is `APPROVED` for exam composition workflow.
- Introduced `JpaSpecificationExecutor` and `QuestionSpecifications` builders for AND-composed question filters.
- Added `QuestionPickerItemResponse` DTO with 200-character question text snippet.
- Runtime release will be tagged `v0.10.0` after production deploy and smoke verification; no tag is created in this batch.

### Phase C1.5-B — Admin question picker Android UI (2026-05-22)

- Added Android admin question picker data wiring for `GET /admin/questions/picker`.
- Added picker Activity, ViewModel, adapter, and layouts for selecting approved questions.
- Wired the admin exam detail screen to display exam questions and launch the picker for DRAFT exams.
- Added JVM unit coverage for picker API contract, ViewModel state, and adapter binding helpers.

### Phase C1.4 CLOSEOUT — Legacy admin question API and editor removed (2026-05-22)

- Removed 5 unused question methods (`getQuestions`, `getQuestionDetail`, `approveQuestion`, `rejectQuestion`, `requestRevision`) from `AdminApi.java`; admin question flow now exclusively uses `AdminQuestionApi`.
- Removed legacy `QuestionBankAdapter.java` after fragment migration to `AdminReviewQueueAdapter`.
- Removed legacy `QuestionEditorActivity.java` and its Manifest entry; admin entry points now route to `CollaboratorCreateQuestionActivity`.
- No backend change; runtime release remains pinned at `v0.9.4`.

### Phase C1.4-A — Admin Review Queue Android UI (2026-05-23)

- Added Android admin question review queue data layer with `AdminQuestionApi`, `AdminReviewActionRequest`, and `AdminQuestionRepository`.
- Added `AdminReviewViewModel` and `AdminReviewQueueAdapter` for real API-backed review queue state.
- Implemented `AdminReviewQuestionActivity` detail/review screen with read-only question rendering, review history, approve, request revision, and reject actions.
- Switched admin question bank screen from mock/legacy adapter flow to real review queue API.
- Redirected admin create-question entry points to `CollaboratorCreateQuestionActivity`.
- Added unit coverage for API contract, POJO serialization, repository behavior, ViewModel state transitions, and review queue adapter binding.

### Phase C1.3 — Collaborator Question Lifecycle Android UI (2026-05-22)

- Closed the Android collaborator question lifecycle UI track across:
  - C1.3-A — typed collaborator question data layer foundation, merged to main at 315eca8.
  - C1.3-B — collaborator question list with status filter and manual paging, merged to main at 87d465b.
  - C1.3-C1 — collaborator create-question activity with cascade subject/topic/subtopic, dynamic option editor, Save Draft, and Submit for Review, merged to main at 1a2851e.
  - C1.3-C2 — collaborator question detail, inline edit, submit-for-review, and review history UI, merged to main at c306a97.
- Replaced the legacy collaborator editor entry point with a deprecated redirect to `CollaboratorCreateQuestionActivity`.
- Removed obsolete legacy collaborator editor fragments/pager and legacy `CollaboratorApi`.
- Backend remains frozen at v0.9.4; no runtime release and no tag in this closeout batch.
- Carry-forward:
  - Subject/Topic/Subtopic name resolution in detail view.
  - Reviewer name resolution.
  - Diff-based `UpdateQuestionRequest`.
  - Image upload.
  - `FILL_IN_BLANK` type support.
  - Admin review actions in Path B / C1.4.
  - AdminDashboardFragment and AdminQuestionBankFragment still reference deprecated `QuestionEditorActivity` until admin scope cleanup.
  - `PageResponse` relocation from `data/model/admin/` to common package.

### Phase C1.2b-C — Android admin exam detail/edit screen (2026-05-19)
- Implement full AdminExamDetailActivity with read-only detail state and DRAFT-only edit mode.
- Add status-based admin exam actions with confirmation for destructive transitions (Discard, Reject, Archive).
- Extend AdminExamViewModel with examDetailState, editModeState, actionResultState LiveData and all status-transition action methods.
- Add unit tests (17 new tests) for ViewModel state machine and action availability mapping for all 5 statuses.

### Docs — Add design system reference (2026-05-19)
- Move the AI/executor design reference document from root `DESIGN.md` to tracked `docs/DESIGN.md`.
- Keep the design reference available for future implementation planning without treating it as repo trash.

### Phase C1.2b-B — Android admin screens fix (2026-05-19)
- `AdminCreateExamActivity` typed create flow + subject dropdown from public Subject API.
- `AdminExamListFragment` real list adapter + status filters (All / DRAFT / PENDING_REVIEW / PUBLISHED / HIDDEN / ARCHIVED) + vanilla manual paging (Load More button).
- `SubjectApi` / `SubjectResponse` / `SubjectRepository` added for GET /subjects.
- `AdminExamDetailActivity` stub added for C1.2b-C handoff.
- Unit tests added: `SubjectApiContractTest`, `SubjectRepositoryTest`, `AdminExamListAdapterTest`.

### Phase C1.2d-1a — SessionService Unit Coverage Expansion

#### Added
- Added behavior-preserving Mockito unit tests in `SessionServiceTest`:
  - `startSession_modeNull_defaultsToMockExam` — locks the existing default-to-`MOCK_EXAM` behavior when the request omits `mode`.
  - `startSession_totalQuestionsNull_defaultsToZero` — locks the existing default-to-`0` behavior when `totalQuestions` is null at start (client populates via client-submit).
  - `clientSubmit_correctEqualsTotal_persistsAllAnsweredZeroWrong` — boundary case asserting `correctCount == totalQuestions` is accepted and persists `wrongCount=0`, `answeredCount=totalQuestions`, `skippedCount=0`, `status=SUBMITTED`.
  - `clientSubmit_timedOutSession_throwsBadRequest` — covers the second non-IN_PROGRESS / non-SUBMITTED terminal state alongside the existing ABANDONED case.
- Strengthened `clientSubmit_abandonedSession_throwsBadRequest` to also assert the `BAD_REQUEST` error code (previously asserted HTTP status only).

#### Notes
- No runtime code changes (`src/main/**` untouched).
- No DTO, controller, service implementation, repository, or entity changes.
- No schema, migration, or `SecurityConfig` changes.
- No smoke script, API error-code, smoke checklist, README, or deploy runbook changes.
- No tag, no Render deploy, no production smoke.
- Smoke and API-docs follow-up deferred to Phase C1.2d-1b.

### Phase C1.2d-1b — Session Smoke + API Docs

#### Added
- Extended `docs/scripts/smoke_sessions.sh` with TC-SESSION-6 (404 `RESOURCE_NOT_FOUND` for unknown sessionId) and TC-SESSION-7 (400 `VALIDATION_FAILED` for invalid client-submit payload where `correctCount > totalQuestions`). Total cases: 5 → 7.
- Added TC-026 and TC-027 to `docs/SMOKE_CHECKLIST.md` backend smoke section. Total backend TCs 10 → 12; total checklist 25 → 27.
- Documented `BAD_REQUEST` (400) for non-IN_PROGRESS session `client-submit` (TIMED_OUT, ABANDONED) and `RESOURCE_NOT_FOUND` (404) for unknown sessionId in `docs/API_ERROR_CODES.md`.

#### Tracking
- Updated C1.2d-1a status from In Progress → Done in `task.md` to reflect actual merge state at `69dc791` (sync gap from previous batch).

#### Notes
- No runtime code changes (`src/main/**` untouched).
- No schema, migration, SecurityConfig, dependency, or build changes.
- No tag created. No Render deploy. No production smoke run.
- Smoke script syntax verified via `bash -n` only; no live execution against production.

### Phase C1.2d Closeout — Status Sync + UTF-8 Enforcement (2026-05-10)

- Flipped `task.md` C1.2d-1b status from 🟡 In progress → ✅ Done; recorded merge ref `78f5759`.
- Added `.gitattributes` rule `*.md text eol=lf working-tree-encoding=UTF-8` to prevent mojibake recurrence (U+FFFD incident from C1.2d-1b initial commit). `working-tree-encoding` requires Git 2.10+ and will fail-fast on checkout if a Markdown file is encoded otherwise.
- (Conditional) Appended IDE encoding configuration note to `docs/DEVELOPER_SETUP.md` if that file existed.
- Docs-only batch. No runtime change. No tag. No deploy. `v0.9.4` (`c4c2993...`) remains the production runtime release.

### Phase C1.2b-PRE-2 — Android Test Infra Setup (2026-05-11)

- Added Android JVM test dependencies: mockito-core, mockwebserver, and androidx.arch.core:core-testing.
- Added C1_2b_PreSmokeTest verifying JUnit, Mockito, MockWebServer, and LiveData executor support.
- Build/test infrastructure only — no runtime Android source, backend, schema, SecurityConfig, Render, or production behavior changed.
- Runtime backend release remains pinned at v0.9.4 (c4c2993deba664883132edf043401e41ffdbca61).

### Phase C1.2b-A — Android Data Layer Foundation (2026-05-15)

- Added admin exam DTO/request POJOs for the Android v0.9.4 contract.
- Aligned AdminApi with all 14 v0.9.4 admin exam endpoints and reused the existing ApiResponse<T> envelope.
- Added PageResponse<T> because no existing Android page wrapper was present.
- Added AdminExamRepository callback wrapper with ApiResponse<T> data unwrapping.
- Added AdminExamViewModel LiveData state foundation for list, detail, create, update, add-question, and reorder flows.
- Added unit tests for POJO JSON, AdminApi MockWebServer contracts, repository result/error paths, and ViewModel LiveData transitions.
- Android data-layer/test batch only — no backend, runtime, schema, tag, deploy, Render, or production behavior changed.

### Phase C1.2b — Closeout (2026-05-19)
- Closed C1.2b Android admin exam CRUD client work on main: PRE-2 (`93c577d`), A (`b604ba7`), B (`2014897`), C (`1dbe386`), plus design-doc relocate (`947bde0`).
- Deferred C1.2b-D to C1.3+ because backend remains frozen at v0.9.4 and Android needs a new picker endpoint with `subjectId` / `questionType` / `q` filters plus `questionText` in DTO.
- Carry-forward: set Windows local repos to `git config core.autocrlf input` to avoid phantom dirty working trees when switching branches.
- Carry-forward: when creating merge commits through Python subprocess on Windows, use ASCII-only commit subjects and avoid em dash to prevent CP1252 mojibake.
- Carry-forward: coordinator merge prompts must explicitly forbid `gh api` for commit/ref operations to avoid local/remote desync.
- No runtime release: backend pin remains v0.9.4, and no tag was created.

## [0.9.4] - 2026-05-06 — Phase C1.2c.1: Admin Exam DRAFT Discard

#### Added
- Added `DELETE /admin/exams/{examId}` for admin DRAFT discard. DRAFT exams are hard-deleted; non-DRAFT statuses return `409 INVALID_STATE`; missing exams return `404 RESOURCE_NOT_FOUND`.
- Authorization follows existing admin exam rules: `CONTENT_ADMIN` and `SUPER_ADMIN` are allowed, anonymous requests return `401`, and `STUDENT` returns `403`.
- Extended `docs/scripts/smoke_admin_exams.sh` to create its own DRAFT exam, discard it, and verify the deleted exam returns `404 RESOURCE_NOT_FOUND`.

#### Verified
- Tagged `v0.9.4` at release commit `c4c2993deba664883132edf043401e41ffdbca61`.
- Production stability watch passed: 5/5 rounds returned 200 for `/actuator/health`, `/exams`, and authenticated `/admin/exams`.
- Production smoke passed all 7 scripts.
- `smoke_admin_exams.sh` passed 12/12 checks, including the new DRAFT discard case.

#### Notes
- No schema, migration, enum, dependency, `SecurityConfig`, or Render config changes.
- Audit implementation is deferred because no active production audit service/repository pattern exists to reuse.
- A prior transient `DELETE /admin/exams/{id}` 500 during smoke was diagnosed as probable deploy-readiness timing after production later passed without a code hotfix.
- No code hotfix was committed.

### Phase C1.2b-3 — Exam Ops Cleanup

#### Operational
- Generalized `.gitattributes` to enforce LF line endings repo-wide for `*.sh`, replacing the single-file `docs/scripts/smoke_admin_exam_composition.sh` rule introduced as a hotfix in `6c2b8fc`. All current and future shell scripts under `docs/scripts/` and `VSAT/**/docs/scripts/` now check out LF on Windows without manual conversion. Closes the `smoke_auth.sh` CRLF follow-up flagged in `docs/DEPLOY_RUNBOOK.md` Smoke Script Runner Notes.
- Renormalized tracked shell scripts under `docs/scripts/` to LF (`git add --renormalize`); only working-tree EOLs change — script bodies are unchanged.
- Documented the `jq`-unavailable fallback contract for the exam-family smoke scripts (`smoke_admin_exams.sh`, `smoke_admin_exam_composition.sh`, and `VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh`) in `docs/DEPLOY_RUNBOOK.md` ("Smoke Script jq Fallback") and surfaced the prerequisites in `docs/SMOKE_CHECKLIST.md`. Notes that `EXAM_ID=<seeded-public-exam-id>` is the required override for `smoke_exams.sh` when `jq` is missing (production v0.9.3 used `EXAM_ID=2`).
- Hardened the real public exam smoke script at `VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh`: validates runnable `jq`, documents optional env vars, logs `EXAM_ID` override use, and fails with an actionable `EXAM_ID=2` fallback instruction when detail checks cannot auto-discover an exam id.
- Added design notes for deferred DRAFT cleanup, LOCKED semantics, and Exam.version / optimistic-locking choices under `docs/design/`.
- No Java source changes; no schema changes; no build changes; no tag created.

## [0.9.3] - 2026-05-06 - Phase C1.2b-2: Exam Composition + Publish Workflow

### Added
- Admin exam composition endpoints:
  - `POST /admin/exams/{examId}/questions`
  - `DELETE /admin/exams/{examId}/questions/{questionId}`
  - `PUT /admin/exams/{examId}/questions/reorder`
- Admin exam workflow endpoints:
  - `POST /admin/exams/{examId}/submit-review`
  - `POST /admin/exams/{examId}/publish`
  - `POST /admin/exams/{examId}/hide`
  - `POST /admin/exams/{examId}/archive`
  - `POST /admin/exams/{examId}/reject-review`
  - `POST /admin/exams/{examId}/return-to-draft`
- Two-phase exam question reorder: rows first move to a negative temporary order range, then final 1-based positive order is applied to avoid `UNIQUE(exam_id, question_order)` collisions.
- `docs/scripts/smoke_admin_exam_composition.sh` smoke script for composition, workflow, public visibility, republish audit overwrite, and archived-state rejection coverage.

### Released
- Tagged `v0.9.3` after production smoke passed.
- Tag target commit: `e2ccd9cc08b8d4d2d48f1549152139e1851bfdb9`.
- PR context:
  - PR #7 merged the C1.2b-2 feature implementation (`2434c11161a4f0cd2d5839146213c88f5da39c34`).
  - PR #8 merged the follow-up smoke-script LF handling guard (`6c2b8fc031c5dd90caff5c7c9ba3fc639ad534a9`).

### Verified
- Production warm-up probes passed before tagging:
  - `/actuator/health`: 200.
  - `/exams`: 200.
  - authenticated `/admin/exams`: 200.
- Render stability watch passed: 10/10 rounds all returned 200 for health, `/exams`, and authenticated `/admin/exams`.
- Production smoke PASS:
  - `smoke_auth.sh` no-register mode: 7 pass / 0 fail / 2 skipped.
  - `smoke_subjects.sh`: 4/4 PASS.
  - `smoke_sessions.sh`: 5/5 PASS.
  - `smoke_questions.sh`: 32/32 PASS.
  - `smoke_exams.sh`: 10/10 PASS.
  - `smoke_admin_exams.sh`: 10/10 PASS.
  - `smoke_admin_exam_composition.sh`: 17 pass / 0 fail / 0 skip / 0 blocked.

### Notes
- `publish` is intentionally `SUPER_ADMIN` only. This is a release bottleneck; production smoke requires at least one active SUPER_ADMIN account.
- Concurrency control for exam workflow/composition transitions is deferred. MVP relies on Postgres READ_COMMITTED + last-write-wins. Optimistic locking via `@Version` to follow once `Exam.version` semantics are verified.
- DRAFT -> ARCHIVED remains intentionally unsupported in this phase; there is no archive path for abandoned drafts.
- `LOCKED` is defined in the enum but unused by business logic; this phase defensively rejects it as a source state and does not transition into it.

## [0.9.2] - 2026-05-05 - Phase C1.2b-1: Admin Exam CRUD Foundation

### Added
- Add Phase C1.2b-1 admin exam CRUD foundation: admin-only create/list/detail/update metadata endpoints for exams (`/admin/exams`, `/admin/exams/{id}`).
- New DTOs `AdminExamCreateRequest`, `AdminExamUpdateRequest`, `AdminExamResponse`, `AdminExamSummaryResponse` with explicit field whitelists; no `questions`, `correctOptionId`, `explanation`, or composition detail exposed by admin metadata endpoints.
- New `AdminExamService` + `AdminExamServiceImpl` — server-controls `status=DRAFT`, `questionCount=0`, `pricingType=FREE`, `price=0`, `version=1`, `createdBy` from authenticated user; `examCode` validated against `^[A-Z][A-Z0-9_]{2,49}$`; metadata update allowed only for `DRAFT`/`HIDDEN` exams; `examCode`, `status`, `questionCount`, `createdBy`, `version`, `publishDate` immutable from client on update.
- `ExamRepository.existsByExamCode(...)` and null-safe derived repository dispatch for admin listing filters (`status`, `subjectId`, or both).
- `AdminExamController` at `/admin/exams` with method-level `@PreAuthorize("hasAnyRole('CONTENT_ADMIN','SUPER_ADMIN')")` (defense-in-depth on top of the existing `/admin/**` security matcher).
- `docs/scripts/smoke_admin_exams.sh` production smoke script for admin exam metadata CRUD.
- 25 Mockito unit tests for `AdminExamService` covering create/update/list/get happy paths, duplicate/invalid `examCode`, non-FREE pricing, non-zero price, missing/inactive subject, invalid-state update on `PUBLISHED`/`ARCHIVED`, list filter dispatch, and DTO field-whitelist enforcement.

### Fixed
- Replaced nullable optional-filter JPQL for `GET /admin/exams` with derived repository dispatch after production PostgreSQL returned `could not determine data type of parameter $1` for `(:status IS NULL OR ...)` style predicates.

### Verified
- Local backend test suite PASS: 104/104 tests.
- Production smoke PASS on Render Singapore:
  - `smoke_admin_exams.sh`: 10/10 PASS.
  - `smoke_auth.sh` no-register mode: 7 pass / 0 fail / 2 skipped.
  - `smoke_subjects.sh`: 4/4 PASS.
  - `smoke_sessions.sh`: 5/5 PASS.
  - `smoke_questions.sh`: 32/32 PASS.
  - `smoke_exams.sh`: 10/10 PASS.
- 5-minute post-deploy stability watch completed: `/actuator/health` 200, `/exams` 200, and authenticated `/admin/exams` 200 through 2026-05-05 08:41:12 UTC.

### Notes
- Exam composition, add/remove/reorder questions, publish workflow (DRAFT → PENDING_REVIEW → PUBLISHED → HIDDEN/ARCHIVED), version bump rules, paid pricing, and access control remain deferred to Phase C1.2b-2.
- No schema, build, or application config changes. Public `/exams` API still exposes `PUBLISHED` + `FREE` only.
- Smoke-created DRAFT admin exams remain as production smoke artifacts because C1.2b-1 intentionally has no delete/archive endpoint.

### Operational
- Add `SMOKE_AUTH_SKIP_REGISTER=1` no-register mode for `docs/scripts/smoke_auth.sh` to allow repeated production regression runs without hitting the `/auth/register` rate limit (HTTP 429). Skipped TCs (TC-AUTH-7, TC-AUTH-8) are clearly reported and the summary distinguishes passed / failed / skipped.
- Document Render free-tier post-deploy warm-up behavior for newly deployed endpoints in `docs/DEPLOY_RUNBOOK.md` Known Pitfalls. Future deploy watches must probe both `/actuator/health` and at least one newly deployed endpoint before tagging a release.
- Document curl JSON quoting pitfall in `docs/DEPLOY_RUNBOOK.md` Known Pitfalls — malformed JSON request bodies during manual probes can produce `HttpMessageNotReadableException`, which must not be confused with an auth/credential failure.
- Track deferred local JDK / Pleiades stash cleanup as operational debt in `task.md` (two preserved stashes from Batch 2b and Phase C1.2a; cleanup awaits explicit user decision).

## [0.9.1] - 2026-05-05 — Phase C1.2a: Exam Read-Only Public API

### Added
- Added `Exam` and `ExamQuestion` JPA entities mapped to frozen schema.
- Added `ExamStatus` and `ExamPricingType` enums (full schema values for
  load-compatibility; only `PUBLISHED` + `FREE` exposed publicly).
- Added `ExamRepository` and `ExamQuestionRepository`.
- Added `ExamService` (interface + impl) — read-only methods.
- Added `ExamPublicController` with `GET /exams` and `GET /exams/{id}`.
- Added `ExamSummaryResponse` and `ExamDetailResponse` with explicit
  field whitelist; correct-option / explanation / questions array NOT
  exposed in any response.
- Added Mockito unit tests for ExamService (12 tests including positive
  and negative DTO field assertions).
- Added idempotent smoke seed `VSAT/vsat-compass-api/docs/scripts/seed_c1_2a_smoke.sql`.
- Added `VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh` production smoke script for the public Exam API.
- SecurityConfig allowlist updated for `GET /exams` and `GET /exams/**`
  only (non-GET methods remain on default auth chain).

### Verified
- Local backend test suite PASS: 79/79 tests.
- Production smoke PASS on Render Singapore:
  - `smoke_exams.sh`: 10/10 PASS.
  - `smoke_subjects.sh`: 4/4 PASS.
  - `smoke_sessions.sh`: 5/5 PASS.
  - `smoke_questions.sh`: 32/32 PASS.
  - `smoke_auth.sh`: register-heavy cases skipped because of known production `429` rate limit behavior.
- 5-minute post-deploy stability watch completed: health 200, `GET /exams` 200, and `GET /exams/{id}` 200 through 2026-05-05 13:18:28 +07:00.

### Notes
- Released from no-ff merge commit `1a87721` on `main`; tag `v0.9.1` points to that deployed code commit, not to the docs/tooling closeout commit.
- No database schema change; C1.2a maps to existing frozen schema.
- Public `/exams` response shape is a Spring paged object under `data.content[]`.
- Render free-tier deploy warm-up was observed: actuator health reached 200 before the new `/exams` endpoint consistently returned 200. Future releases should allow a 3-5 minute post-deploy warm-up window before asserting newly added endpoint health.
- Smoke login probes should build JSON bodies with Bash-contained `printf` or equivalent structured serialization; Windows shell quoting can malformed curl JSON and produce misleading `HttpMessageNotReadableException` failures.
- Admin exam CRUD, composition, status workflow, paid/package pricing, and Android production Exam API integration remain deferred.

## [0.9.0] - 2026-05-05 — Phase C1.1b: Question CRUD Workflow + Role-Based Authorization

### Added
- Collaborator Question CRUD workflow:
  - `POST /collaborator/questions`
  - `GET /collaborator/questions`
  - `GET /collaborator/questions/{id}`
  - `PUT /collaborator/questions/{id}`
  - `POST /collaborator/questions/{id}/submit-for-review`
- Admin question review workflow:
  - `GET /admin/questions?status=...`
  - `POST /admin/questions/{id}/approve`
  - `POST /admin/questions/{id}/request-revision`
  - `POST /admin/questions/{id}/reject`
- Role-based authorization for Question Bank endpoints using HTTP security rules + method-level `@PreAuthorize` + service-layer owner checks.
- Question status state machine for `DRAFT`, `NEEDS_REVISION`, `PENDING_REVIEW`, `APPROVED`, and `ARCHIVED` transitions.
- `VSAT/vsat-compass-api/docs/scripts/smoke_questions.sh` production smoke script for collaborator/admin question workflow.

### Verified
- Local backend test suite PASS: 67/67 tests.
- Production smoke PASS on Render Singapore:
  - `smoke_auth.sh`: 9/9 PASS earlier in the same C1.1b.2 release run. A later auth rerun hit expected `429 RATE_LIMIT_EXCEEDED` on register-heavy TC-AUTH-7/8 after repeated smoke account setup; not treated as an Auth regression.
  - `smoke_sessions.sh`: 5/5 PASS.
  - `smoke_subjects.sh`: 4/4 PASS.
  - `smoke_questions.sh`: 32/32 PASS.
- 5-minute post-deploy stability watch completed: health UP, valid student login PASS, `GET /auth/me` with Bearer PASS, and `smoke_questions.sh` rerun 32/32 PASS.

### Notes
- Released from merged C1.1b code on `main` and tagged `v0.9.0` at deployed code commit `752c15e`.
- No database schema change; C1.1b maps to existing frozen schema from C1.1a.
- Production smoke required minimal taxonomy seed data (`MATH_SMOKE_ALGEBRA`, `MATH_SMOKE_LINEAR`) and qbank smoke role accounts.
- `question_versions`, `question_groups`, publication scheduling, Excel import, and exam composition remain deferred to later Phase C batches.
- Generated smoke test questions remain in production as test data because no delete/archive endpoint exists in C1.1b.

## [0.8.4] - 2026-05-04 — Phase C1.1a: Question Bank Schema Foundation

### Added (Phase C1.1a — production)
- 4 enum types mapped to Postgres ENUMs: `Difficulty` (→ `difficulty_level`), `QuestionType`, `QuestionStatus`, `ReviewAction`. Mapped via `@Enumerated(EnumType.STRING) + @JdbcTypeCode(SqlTypes.NAMED_ENUM) + columnDefinition` (Phase B `User.role` pattern).
- 4 JPA entities mapped to existing frozen schema: `Subtopic`, `Question`, `QuestionOption`, `QuestionReview`. FK-as-Long convention; no bidirectional relations.
- 4 repositories with the query methods C1.1b will exercise (paged listings on `Question`, derived deletes on `QuestionOption`, etc.).
- `SubtopicResponse` DTO + `SubtopicService` interface + `SubtopicServiceImpl`.
- New public endpoint: `GET /subjects/{subjectId}/topics/{topicId}/subtopics` (subject + topic existence + topic-belongs-to-subject validation).
- `SubtopicServiceTest` — 6 Mockito unit tests covering happy path, 404 propagation order (subject-first), topic/subject mismatch (400 VALIDATION_FAILED), empty list, DTO field selection.

### Verified
- 34/34 tests PASS locally on JDK 21 (10 AuthService + 8 SessionService + 10 SubjectService + 6 SubtopicService).
- **Production smoke 18/18 PASS** on Render Singapore at 2026-05-04 UTC against `https://vsat-compass-api.onrender.com/api/v1`:
  - `smoke_subjects.sh` 4/4 (TC-SUBJ-1 → TC-SUBJ-4, includes new subtopics endpoint)
  - `smoke_auth.sh` 9/9 (TC-AUTH-1 → TC-AUTH-9, no regression vs v0.8.3)
  - `smoke_sessions.sh` 5/5 (TC-SESSION-1 → TC-SESSION-5, no regression vs v0.8.3)

### Notes
- Released on `main` via PR #4 merge commit `f5f03e1` and tagged `v0.8.4`.
- No write endpoints, no role-based authorization in this batch — those land in C1.1b (target tag `v0.9.0`).
- No schema change; no dependency change. SecurityConfig unchanged (existing `/subjects/**` GET permitAll covers the new endpoint).
- Render auto-deploy completed 2026-05-04 from `main`; production now exposes the new Subtopics endpoint.
- Feature branch `phase-c/c1-1a-question-bank-schema` deleted (local + remote) after merge + smoke verification.

## [0.8.3] - 2026-05-04 — Phase C1.0: Subject + Topic Foundation (Read-Only)

### Added (Phase C1.0)
- `Subject` and `Topic` JPA entities mapped to existing `subjects` and `topics` Postgres tables.
- `SubjectRepository`, `TopicRepository` with active-only ordered query methods.
- `SubjectService` interface + `SubjectServiceImpl` for read-only listing.
- Public endpoints: `GET /subjects`, `GET /subjects/{id}/topics` (wrapped in `ApiResponse` envelope).
- `SubjectServiceTest` — 10 Mockito unit tests covering listing, ordering, 404 propagation, and DTO field selection.
- No SecurityConfig change needed — existing `requestMatchers(GET, "/subjects/**").permitAll()` already covers both endpoints.

### Tests
- `docs/scripts/smoke_subjects.sh` — 3 TCs covering the new public endpoints (TC-SUBJ-1 list, TC-SUBJ-2 valid topics, TC-SUBJ-3 404 RESOURCE_NOT_FOUND). Shipped as chore PR #3 on `chore/c102-smoke-script`, merge commit `91a90ff`.

### Verified
- 28/28 unit tests PASS locally (10 AuthService + 8 SessionService + 10 SubjectServiceTest).
- **Production smoke 17/17 PASS** on Render Singapore at 2026-05-04 09:49 UTC against `https://vsat-compass-api.onrender.com/api/v1`:
  - `smoke_subjects.sh` 3/3 (TC-SUBJ-1 → TC-SUBJ-3)
  - `smoke_auth.sh` 9/9 (TC-AUTH-1 → TC-AUTH-9, no regression vs v0.8.2)
  - `smoke_sessions.sh` 5/5 (TC-SESSION-1 → TC-SESSION-5, no regression vs v0.8.2)

### Notes
- Released on `main` via no-ff merge commit `6319766` (PR #2 Phase C1.0 feature) and tagged `v0.8.3` at that commit. Smoke-script chore PR #3 (`91a90ff`) ships unversioned on `main` — tag deliberately points at the feature merge to keep release history aligned with shipped code rather than tooling.
- No schema change: the entities map to existing frozen schema columns.
- Render auto-deploy completed 2026-05-04 from `main`; production now exposes `/subjects` and `/subjects/{id}/topics`.
- Feature branch `phase-c/c1-0-subject-topic` and chore branch `chore/c102-smoke-script` deleted (local + remote) after merge + smoke verification.

### Documentation
- Add Phase C precheck audit report (`docs/audit/PHASE_C_PRECHECK_REPORT.md`) — read-only baseline before Phase C kickoff. No source changes. *(prior audit commit, shipped with this release)*

### Repo Hygiene (Batch 1, shipped with 0.8.3)
- Tag retroactive releases v0.5.0 through v0.8.1 (6 annotated tags) so previous CHANGELOG versions are checkout-able.
- Add `.claude/`, `local.properties`, `*.keystore`, `*.jks` to root `.gitignore`.
- Untrack `.claude/settings.json` and `.claude/settings.local.json` from the repository (files preserved on disk).
- Delete stale local branch `backup-before-author-fix-20260410` (commit `009350a` is the pre-rebase equivalent of `ed2637e` / `v0.5.0`).
- Update `docs/SMOKE_CHECKLIST.md` version stamp to `v0.8.1 / 2026-04-26`.

## [0.8.2] - 2026-04-30 — Spring Boot 3.5.9 Upgrade (Batch 2b)

### Changed
- **Spring Boot 3.2.5 → 3.5.9** (latest 3.5.x patch). 3.2 line reached EOL Nov 2024;
  3.5 has OSS support through Jun 30, 2026. Major version 3.x preserved (no API surface change).
- Spring Security `6.2.4 → 6.5.7` (transitively via Spring Boot BOM).
- Spring Framework `6.1.x → 6.2.15`.
- Hibernate ORM `6.4.4.Final → 6.6.39.Final`.
- Tomcat embed `10.1.20 → 10.1.50`.
- Jackson `2.15.4 → 2.19.4`.
- PostgreSQL JDBC `42.6.2 → 42.7.8` (resolves audit MEDIUM #7 transitively).
- Pinned deps unchanged: jjwt 0.12.5, bucket4j 8.10.1, mapstruct 1.5.5.Final, springdoc-openapi 2.5.0.

### Verified
- All 18 Batch 2a characterization tests still PASS on 3.5.9 (zero behavior regression detected at service layer).
- Local `bootRun` startup: actuator health UP, JVM started in 7s, no ERROR-level startup log entries (one benign WARN about explicit PostgreSQLDialect property — left for a follow-up since `src/main` was out of scope for this run).
- Local `smoke_auth.sh`: 9/9 PASS against `http://localhost:8080/api/v1` (TC-AUTH-1 through TC-AUTH-9).
- **Production smoke 14/14 PASS** (`smoke_auth.sh` 9/9 + `smoke_sessions.sh` 5/5) on Render Singapore at 2026-04-30 20:09 ICT, post merge commit `0a0f341`.

### Tests (originally drafted as Batch 2a, ships with 0.8.2)
- Add `AuthServiceTest` — 10 Mockito unit tests covering register (happy path + duplicate email), login (happy path, wrong password, non-existent email, non-ACTIVE account), refreshToken (rotate + revoke old, unknown/revoked token, expired token), and logout. Re-verified on Spring Boot 3.5.9.
- Add `SessionServiceTest` — 8 Mockito unit tests covering startSession (happy path, invalid mode), clientSubmit (happy path with client-trusted score persistence, unknown sessionId 404, different user 403, anti-replay 409, abandoned session 400, correctCount > totalQuestions VALIDATION_FAILED). Re-verified on Spring Boot 3.5.9.
- Repository slice tests via Testcontainers — DEFERRED to Batch 2a-bis pending Docker Desktop / WSL2 availability on dev machine (VMware/Hyper-V conflict — `bcdedit hypervisorlaunchtype=off` set by VMware Workstation).

### Notes
- Released on `main` via no-ff merge commit `0a0f341` and tagged `v0.8.2`. Render auto-deploy completed 2026-04-30; production now runs Spring Boot 3.5.9.
- Backup branch `backup-pre-batch-2b-20260430` retained at the pre-upgrade `main` commit (`1f88ee2`) through end of May 2026 — emergency rollback path is `git revert -m 1 0a0f341 && git push origin main` (Render auto-deploys back to 3.2.5).
- Spring Boot 3.5 → 4.0 upgrade is tracked separately as Batch 2c (post-Phase C).

### Resolved audit findings
- HIGH #2 — Spring Boot EOL upgrade (3.2.5 → 3.5.9 in production)
- MEDIUM #7 — PostgreSQL driver patched transitively via SB BOM (42.6.2 → 42.7.8)

## [0.8.1] - 2026-04-25 — Phase B Production Hardening

### Bug Fixes

- **fix(auth): GET /auth/me, PUT /auth/me, PUT /auth/me/password returned 500 instead of 401
  when Bearer token was missing.**
  Root cause: `SecurityConfig` used `.requestMatchers("/auth/**").permitAll()` which
  allowed unauthenticated requests through to the controller. The controller called
  `SecurityUtils.getCurrentUserId()` → `null` → `findById(null)` → `IllegalArgumentException` → 500.
  Fix: tightened the permitAll allowlist to `/auth/login`, `/auth/register`, `/auth/refresh`,
  `/auth/logout` only. Spring Security now rejects unauthenticated `/auth/me` at the filter
  layer before the controller is reached.
  Shipped in commit `24b73af` — CHANGELOG entry backfilled here.

- **fix(security): Spring Security returned 403 instead of 401 for missing Bearer token.**
  RESTful convention requires 401 (Unauthorized) for missing/invalid auth and 403 only
  for authenticated-but-lacks-role cases. Spring Security's default `ExceptionTranslationFilter`
  produces 403 when no `AuthenticationEntryPoint` is configured.
  Fix: added `restAuthenticationEntryPoint()` bean to `SecurityConfig` that returns 401
  with the standard `ApiResponse.error()` envelope (`AUTH_UNAUTHORIZED`). Wired via
  `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))`.

- **fix(api): POST /sessions/start returned 500 INTERNAL_ERROR when `exam_id` FK was violated.**
  Root cause: `GlobalExceptionHandler` had no handler for `DataIntegrityViolationException`,
  which fell through to the generic `Exception` handler → 500.
  Fix: added `@ExceptionHandler(DataIntegrityViolationException.class)` returning
  400 `DATA_INTEGRITY_VIOLATION`. Future callers sending a non-existent `examId` now
  receive a clear 400 instead of a misleading 500.
  Root trigger: `exams` table was empty (Exam management is Phase C scope).
  Resolved via manual SQL seed (see `docs/seed/smoke_test_seed.sql`).

### Documentation

- **docs/seed/smoke_test_seed.sql** — new. Idempotent SQL seed for smoke testing the
  Session API. Inserts subject `MATH` and placeholder exam `SMOKE_001`. Run once in
  Neon Console before executing `smoke_sessions.sh`. Will be replaced by Phase C fixtures.
- **docs/scripts/smoke_sessions.sh** — updated to use `EXAM_ID` env var (default `1`)
  instead of hardcoded `examId=1`. Override with `EXAM_ID=<id>` if seed assigns a
  different ID.
- **docs/API_ERROR_CODES.md** — added `DATA_INTEGRITY_VIOLATION` (400) error code entry.

### Neon DB Verification (2026-04-25)

| Table | Result |
|-------|--------|
| `users` | 4 seeded accounts confirmed (STUDENT, COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN) all status=ACTIVE |
| `refresh_tokens` | 6 tokens created during smoke window (14:01–14:36 UTC); mix of revoked=true (logout TC) and revoked=false; expires_at = +30 days |
| `exam_sessions` | 2 rows: session id=4 (MOCK_EXAM, SUBMITTED, score=73.33, correct=22/30) + id=5 (PRACTICE, IN_PROGRESS) |
| `exam_sessions` orphans | 0 — all sessions correctly linked to user_id=4 (student@vsat.com) and exam_id=1 (SMOKE_001) |

- Anti-replay verified at both HTTP (409) and DB levels (`status=SUBMITTED` prevents re-submit)
- Owner check verified at both HTTP (403) and DB (`user_id` enforced in service layer)
- Foreign key integrity: `exam_id` → `exams.id` confirmed via 0 orphan sessions

### Notes
- Phase B production smoke verification: **14/14 PASS** (9 auth + 5 session) on 2026-04-25
- Anti-replay verified: duplicate `/sessions/{id}/client-submit` returns 409 SESSION_ALREADY_SUBMITTED
- TC-SESSION-2: `/sessions/start` without Bearer now returns 401 (AuthenticationEntryPoint active)
- Phase B officially closed 2026-04-25

### Operational Status
- API: LIVE at `https://vsat-compass-api.onrender.com/api/v1/`
- Database: CONNECTED (Neon PostgreSQL pooler, ap-southeast-1)
- Android client: USING PRODUCTION (`BASE_URL_CLOUD` verified)
- Monitoring: ACTIVE (UptimeRobot 5-minute interval)

---

## [0.8.0] - 2026-04-23 — Phase B: Backend Production-Ready

### Mục tiêu
Stabilize public backend, lock in minimum required APIs (Auth + Session sync), harden security — nền tảng cho Phase C (admin content management).

---

### 🏗️ Phase 1 — Public Backend Stability

#### Actuator & Monitoring
- [x] Thêm `spring-boot-starter-actuator` — expose chỉ `/actuator/health` (no details)
- [x] Thêm `bucket4j-core` dependency (chuẩn bị cho Phase 3 rate limiting)

#### Config Hardening (`application.yml`)
- [x] **Remove JWT_SECRET fallback** — app fail-fast nếu env var chưa set
- [x] CORS driven by `CORS_ALLOWED_ORIGINS` env var (default: localhost dev)
- [x] Logging levels tối ưu: prod `WARN` cho Hibernate SQL + Spring Security
- [x] Dev profile: `HIBERNATE_SQL_LOG` env var toggle

#### Security (`SecurityConfig.java`)
- [x] HSTS header enabled (max-age 1 year, includeSubDomains)
- [x] Actuator health endpoint added to public permits
- [x] Rate limit filter registered before JWT filter

#### Secret Management
- [x] **`render.yaml`**: Remove tất cả hardcoded secrets (JWT_SECRET, DATABASE_URL, DATABASE_USERNAME)
- [x] Chuyển sang Render Dashboard env var management
- [x] Health check path configured: `/api/v1/actuator/health`
- [x] `.env.example` cập nhật: CORS_ALLOWED_ORIGINS, stronger warnings

#### Response Envelope
- [x] Thêm `timestamp` field (ISO-8601 OffsetDateTime) vào `ApiResponse`

#### Documentation
- [x] **`docs/DEPLOY_RUNBOOK.md`** — 7 sections: prerequisites, first-time deploy, routine deploy, rollback, secret rotation, incident triage, cost watch

---

### 🔌 Phase 2 — Minimum Required APIs

#### Session Module (NEW — 8 files)
- [x] `ExamSession` entity mapped to frozen `exam_sessions` table schema
- [x] `SessionMode` enum: `MOCK_EXAM`, `PRACTICE`
- [x] `SessionStatus` enum: `IN_PROGRESS`, `SUBMITTED`, `TIMED_OUT`, `ABANDONED`
- [x] `ExamSessionRepository`: `findById`, `findByIdAndUserId`
- [x] `SessionRequest` DTO: `StartSession`, `ClientSubmit` (với validation)
- [x] `SessionResponse` DTO: `SessionInfo`
- [x] `SessionService` + `SessionServiceImpl`:
  - `startSession()` — tạo phiên thi mới
  - `clientSubmit()` — nộp kết quả client-side scoring
  - Anti-replay: 409 `SESSION_ALREADY_SUBMITTED` nếu đã nộp
  - Owner check: 403 `SESSION_FORBIDDEN` nếu không phải chủ phiên
  - Validation: `correctCount ≤ totalQuestions`, `score 0-100`, `timeSpent 0-86400`
- [x] `SessionController`: `POST /sessions/start` (201), `POST /sessions/{id}/client-submit` (200)

#### Error Code Standardization
- [x] `AppException` — 10 specific factory methods:
  - Auth: `authEmailTaken()`, `authInvalidCredentials()`, `authUnauthorized()`, `authRefreshInvalid()`, `authForbidden()`
  - Session: `sessionAlreadySubmitted()`, `sessionForbidden()`
  - Generic: `validationFailed()`, `rateLimitExceeded()`, `badRequest()`
- [x] `AuthServiceImpl` — thay generic errors bằng specific error codes

#### Smoke Test Scripts
- [x] `docs/scripts/smoke_auth.sh` — 9 test cases (bash 3.2 compatible)
- [x] `docs/scripts/smoke_sessions.sh` — 5 test cases (bash 3.2 compatible)

---

### 🔒 Phase 3 — Security & Stability Hardening

#### Validation Hardening (`AuthRequest.java`)
- [x] Password: `@Size(min=6)` → `@Size(min=8, max=100)` + `@Pattern(letter+digit)`
- [x] Email: thêm `@Size(max=255)`
- [x] FullName: `@Size(min=2, max=100)`
- [x] Áp dụng cho Register, ChangePassword, ResetPassword

#### Exception Handler (`GlobalExceptionHandler.java`)
- [x] `VALIDATION_ERROR` → `VALIDATION_FAILED`
- [x] Validation errors trả `fieldErrors` map trong `data` field
- [x] Thêm handlers: `BadCredentialsException`, `AccessDeniedException`, `AuthenticationException`

#### Rate Limiting (`RateLimitFilter.java`)
- [x] In-memory Bucket4j rate limiting:
  - `/auth/login`: 10 req/phút/IP
  - `/auth/register`: 5 req/giờ/IP
  - `/auth/refresh`: 30 req/phút/IP
- [x] Client IP resolution: `X-Forwarded-For` first IP (Render proxy)
- [x] Returns 429 `RATE_LIMIT_EXCEEDED` with standard ApiResponse

#### JWT Cleanup Job
- [x] `@EnableScheduling` trên `VsatCompassApiApplication`
- [x] `RefreshTokenCleanupService`: chạy 03:00 AM daily
- [x] Xóa expired tokens + revoked tokens cũ hơn 7 ngày

#### Documentation
- [x] **`docs/API_ERROR_CODES.md`** — Error catalog + response envelope + rate limits + Android handling guide
- [x] **`docs/SMOKE_CHECKLIST.md`** — 10 backend TCs mới (TC-016 → TC-025), tổng 25 TCs
- [x] Android direct-DB audit: **CLEAN** (không có PostgreSQL/JDBC/Neon references)

---

### 📊 Build Results

| Module | Command | Result |
|--------|---------|--------|
| Backend | `./gradlew build -x test` | ✅ BUILD SUCCESSFUL (no warnings) |

### 📁 Files Changed

| Category | Count | Chi tiết |
|----------|-------|----------|
| New files | 15 | 8 Session module + 2 smoke scripts + RateLimitFilter + CleanupService + 3 docs |
| Modified files | 12 | build.gradle, application.yml, SecurityConfig, render.yaml, .env.example, ApiResponse, AppException, AuthServiceImpl, AuthRequest, GlobalExceptionHandler, VsatCompassApiApplication, RefreshTokenRepository |

---

## [0.7.1] - 2026-04-23 — Phase A Polish & Hardening

### Mục tiêu
Polish toàn diện Phase A (Local Pack + Exam Review + History) trước khi bước sang Giai đoạn B. Build pass + 6 unit tests pass.

---

### 🔍 Phase 1 — Code Quality Audit & Fix

**Lint baseline:** 54 errors / 537 warnings (pre-existing). Phase A chỉ thêm 1 error (`MissingSuperCall`) đã được fix.

#### strings.xml
- [x] Extract toàn bộ chuỗi tiếng Việt hardcoded trong Phase A sang `res/values/strings.xml`
- [x] Thêm 30+ string keys: `review_*`, `history_*`, `home_greeting_*`, `home_stats_*`, `time_*`, `empty_state_*`, `cd_*` (accessibility)

#### ScoreConstants.java *(mới)*
- [x] `VSAT_MAX_SCORE = 1200`, `PERCENT_TO_VSAT = 12`, `WEAK_TOPIC_THRESHOLD_PERCENT = 60`
- [x] `ExamResultActivity` và `ExamHistoryEntry` dùng `ScoreConstants` thay vì magic number 12

#### TAG constants
- [x] Thêm `private static final String TAG = "ClassName"` vào: `ExamReviewActivity`, `ExamHistoryActivity`, `ExamHistoryRepository`, `HomeFragment`

#### Exception handling cleanup
- [x] `ExamReviewActivity`: `catch (Exception ignored)` → `catch (JsonSyntaxException e)` + `Log.w(TAG, ...)`
- [x] `ExamHistoryRepository.saveSync()`: `catch (IOException ignored)` → `Log.e(TAG, ..., e)` với context
- [x] `ExamHistoryRepository.loadSync()`: silent swallow → log warning + corrupt file recovery

#### MissingSuperCall
- [x] `ExamSessionActivity.onBackPressed()`: thêm `@SuppressWarnings("MissingSuperCall")` với comment giải thích

#### Schema migration-friendly
- [x] `ExamHistoryEntry`: thêm `@SerializedName` cho mọi field — JSON cũ parse OK khi thêm field mới

#### ExamHistoryAdapter
- [x] `SimpleDateFormat` dùng `Locale.forLanguageTag("vi-VN")` thay vì deprecated constructor

---

### 🛡️ Phase 2 — Robustness & Edge Cases

#### ExamHistoryRepository
- [x] **Atomic write**: ghi vào `.tmp` → `fos.getFD().sync()` → `renameTo()` target — bảo vệ khỏi half-written nếu app bị kill
- [x] **Corrupt file recovery**: `JsonSyntaxException` → rename sang `exam_history.json.corrupt.<timestamp>`, log warning, trả về empty list
- [x] **Concurrent access**: tất cả I/O đi qua `synchronized (fileLock)` + single-thread executor
- [x] **`saveEntry` overload** nhận `Runnable onSaveFailed` → callback khi save thất bại (storage đầy...)
- [x] **`injectMockEntries(count, onDone)`**: debug-only API inject N entries giả để test stress/scroll
- [x] **`clearAll(onDone)`**: debug-only API xóa toàn bộ history
- [x] `Context appCtx = context.getApplicationContext()` tránh Activity context leak trong executor

#### ExamReviewActivity
- [x] Guard `examId == 0` → `finish()` + Toast trước khi inflate
- [x] Guard `questionIds.isEmpty()` sau load → `finish()` + Toast
- [x] `optionExistsInQuestion()`: kiểm tra `selectedOptionId` còn hợp lệ trong pack hiện tại — nếu pack đề đã thay đổi sau khi lưu, treat như câu chưa làm + log warning
- [x] Clamp `currentIndex` khi restore từ `savedInstanceState`

#### ExamHistoryActivity
- [x] `onSaveInstanceState` / restore `currentSubjectFilter` sau rotate
- [x] `syncChipUi()` tách rời khỏi `selectChip()` để restore không trigger reload thừa
- [x] `showLoadingState(true/false)` toggle `ProgressBar` + `RecyclerView`
- [x] `loadHistory()` phân biệt 2 empty state: "chưa có bài nào" vs "filter ra 0 kết quả"
- [x] `openReview()` bọc trong try-catch, log warning nếu `examId` không tìm thấy

#### ExamResultActivity
- [x] Nhận extra `history_save_failed` từ `ExamSessionActivity`
- [x] Hiển thị `Snackbar` cảnh báo khi lưu lịch sử thất bại

#### Debug Dev Menu (ProfileFragment — DEBUG build only)
- [x] Long-press tên hiển thị trong ProfileFragment → dialog dev tools
- [x] "Inject 50 lịch sử mock" → gọi `injectMockEntries(50)`
- [x] "Xóa toàn bộ lịch sử" → gọi `clearAll()`
- [x] Toàn bộ code trong `if (!BuildConfig.DEBUG) return;` — release build không có

---

### ✨ Phase 3 — UX Polish & Verification

#### RelativeTimeHelper.java *(mới)*
- [x] `format(context, timestampMillis)`: < 1ph → "vừa xong"; < 1h → "X phút trước"; < 24h → "X giờ trước"; < 7 ngày → "X ngày trước"; còn lại → "dd/MM/yyyy HH:mm" locale vi-VN
- [x] `ExamHistoryAdapter` dùng `RelativeTimeHelper.format()` thay vì `SimpleDateFormat` tĩnh

#### Loading & Empty states
- [x] `ExamHistoryActivity`: `ProgressBar` (id `progressLoading`) hiện khi đang fetch, ẩn khi xong
- [x] Empty state text khác nhau: "Chưa có bài thi nào" vs "Không có bài thi nào ở môn này"
- [x] `HomeFragment`: stat cards hiện "--" / "0ph" thay vì "0" khi chưa có lịch sử

#### Accessibility
- [x] `activity_exam_review.xml` nút back: `contentDescription="@string/cd_back"` + `minWidth/Height="48dp"`
- [x] `activity_exam_history.xml` nút back: `contentDescription="@string/cd_back"` + `minWidth/Height="48dp"`

#### StrictMode (debug build)
- [x] `VsatApp.enableStrictMode()`: `detectDiskReads + detectDiskWrites + detectNetwork + penaltyLog`
- [x] Chỉ bật khi `BuildConfig.DEBUG` — release build không bị ảnh hưởng

#### Unit Tests
- [x] `ExamHistoryRepositoryTest.java` — 6 test cases (5 yêu cầu + 1 bonus):
  - `saveEntry_thenGetAll_returnsNewestFirst`
  - `saveEntry_exceedsMax_capsAt200`
  - `getByExamId_returnsOnlyMatchingSubject` (filter contains)
  - `getStats_calculatesAverageCorrectly` (avgScore = 900 khi 2 bài 600+1200)
  - `getAll_corruptFile_returnsEmptyAndCreatesCorruptBackup`
  - (TC6: khởi tạo fresh repo mỗi test qua reflection)
- [x] `build.gradle.kts`: `testOptions { unitTests { isReturnDefaultValues = true } }` để Android stubs không throw

#### Smoke Checklist
- [x] `docs/SMOKE_CHECKLIST.md` — 15 test cases bao phủ: login, 5 đề, filter, làm bài, review, history, persist, rotate, empty state, dev menu, sync status, thoát giữa chừng

### Giả định kỹ thuật đã chọn
- **`returnDefaultValues = true`**: Android stubs trong JVM unit test trả về default thay vì throw — đủ để test file I/O logic không cần Robolectric.
- **FakeContext extends ContextWrapper**: không cần Mockito, chỉ override `getFilesDir()`.
- **Không thêm dependency mới**: tất cả fix dùng JUnit4 + standard Java — thoả ràng buộc.

### Kết quả đo lường
| Metric | v0.7.0 | v0.7.1 |
|--------|--------|--------|
| Unit tests pass | 1/1 (ExampleTest) | 6/6 |
| Lint Phase A errors | 1 (MissingSuperCall) | 0 |
| Strings hardcoded (Phase A Java) | ~18 | 0 |
| Exception swallow (silent) | 3 | 0 |

---

## [0.7.0] - 2026-04-23 — Student MVP hoàn chỉnh: Review chi tiết + Lịch sử bài làm

### Tổng quan

Hoàn tất 3 task P0 cuối cùng của Giai đoạn A:

- **A2** — 5 pack đề local với metadata chuẩn hóa, filter chip hoạt động đúng
- **A4** — Màn xem lời giải chi tiết từng câu sau bài thi
- **A5** — Lịch sử bài làm persist local + dashboard student hiển thị dữ liệu thật

Học viên có thể: đăng nhập → làm bài → xem kết quả → xem lời giải → xem lịch sử → tất cả chạy offline.

---

### 📦 Phase 1 — Bổ sung pack đề + chuẩn hóa metadata (A2)

#### Assets mới / cập nhật
- [x] **`sample_math_exam_2.json`** — Đề Toán nâng cao (id=4), 30 câu, 60 phút, MATH_002; topics: số phức, logarithm, hàm số, đạo hàm, tích phân, hình học, xác suất
- [x] **`sample_english_exam_2.json`** — Đề Tiếng Anh nâng cao (id=5), 30 câu, 45 phút, ENG_002; topics: ngữ pháp, từ vựng, đọc hiểu, nhận biết lỗi
- [x] **`sample_english_exam.json`** — Sửa `subject_name: "Tieng Anh"` → `"Tiếng Anh"`, cập nhật nội dung và explanation tiếng Việt
- [x] **`sample_physics_exam.json`** — Sửa `subject_name: "Vat ly"` → `"Vật lí"`, cập nhật nội dung tiếng Việt

Kết quả: filter chip "Toán" → 2 đề, "Tiếng Anh" → 2 đề, "Vật lí" → 1 đề. Tổng 5 pack.

---

### 🔍 Phase 2 — Xem lời giải chi tiết (A4)

#### File mới
- [x] **`ui/exam/session/ExamReviewActivity.java`** — Màn review từng câu: option đúng tô xanh, option sai tô đỏ, explanation đầy đủ, điều hướng ← / →, grid tổng quan
- [x] **`ui/exam/session/ReviewGridAdapter.java`** — Grid adapter cho review: đúng=xanh lá, sai=đỏ nhạt, chưa làm=xám
- [x] **`res/layout/activity_exam_review.xml`** — Layout màn review với ScrollView, option cards, explanation card, bottom nav bar
- [x] **`res/drawable/bg_badge_warning.xml`** — Badge cam cho câu "chưa trả lời"

#### File sửa
- [x] **`ExamSessionActivity.java`** — Thêm serialize `selectedAnswers` → JSON, truyền `examId`/`examTitle`/`examSubject`/`selectedAnswersJson` sang ExamResultActivity; import Gson
- [x] **`ExamResultActivity.java`** — Nhận extras từ Session, nối nút "Xem lời giải chi tiết" → mở ExamReviewActivity với đúng extras
- [x] **`AndroidManifest.xml`** — Đăng ký ExamReviewActivity

**Luồng dữ liệu:** ExamSessionActivity → (intent extras) → ExamResultActivity → ExamReviewActivity. ExamReviewActivity tự load câu hỏi từ LocalExamDataSource theo `examId`, không cần snapshot câu hỏi.

**Tính năng:**
- Option đúng + user chọn đúng → xanh #E8F5E9, icon ✓
- Option đúng + user không chọn → xanh nhạt + label "Đáp án đúng"
- Option sai + user đã chọn → đỏ nhạt + icon ✗ + label "Của bạn"
- Câu chưa làm → badge cam "Bạn chưa trả lời câu này"
- Rotate device → giữ nguyên câu đang xem (onSaveInstanceState)

---

### 📊 Phase 3 — Lịch sử bài làm + Dashboard student (A5)

#### File mới
- [x] **`data/model/ExamHistoryEntry.java`** — POJO: id, examId, examTitle, subject, totalQuestions, correctCount, score (1200), timeSpentSeconds, submittedAtMillis, selectedAnswersJson
- [x] **`data/repository/ExamHistoryRepository.java`** — Lưu JSON vào `filesDir/exam_history.json`; cap 200 entries; methods async (ExecutorService + Handler): saveEntry, getAll, getRecent, getBySubject, getStats, getBestScoreForExam
- [x] **`ui/history/ExamHistoryActivity.java`** — Màn lịch sử: filter chips theo môn, stats row (bài đã làm / điểm TB / tỷ lệ đúng), empty state thân thiện, nút "Xem lại" → ExamReviewActivity
- [x] **`ui/history/ExamHistoryAdapter.java`** — RecyclerView adapter cho list lịch sử
- [x] **`res/layout/activity_exam_history.xml`** — Layout màn lịch sử
- [x] **`res/layout/item_exam_history.xml`** — Card item: tên đề, môn badge, điểm lớn, ngày/giờ, câu đúng, thời gian, nút "Xem lại"

#### File sửa
- [x] **`ExamSessionActivity.java`** — Sau khi chấm điểm, tạo ExamHistoryEntry và gọi `ExamHistoryRepository.saveEntry()` async
- [x] **`HomeFragment.java`** — `loadHistoryStats()` lấy stats thật từ repo: tvTotalExams = số bài đã làm, tvAvgScore = điểm TB V-SAT, tvTotalTime = tổng thời gian; `loadRecentForContinue()` cập nhật section "Tiếp tục luyện tập" theo đề gần nhất; nút "Xem tất cả" → ExamHistoryActivity; `onResume()` reload stats
- [x] **`fragment_home.xml`** — Thêm id `tvViewAllHistory` cho nút "Xem tất cả"
- [x] **`AndroidManifest.xml`** — Đăng ký ExamHistoryActivity

### Giả định kỹ thuật đã chọn
- **Không dùng Room** — dữ liệu lịch sử lưu dạng JSON file trong `filesDir`. Lý do: tránh thêm dependency nặng; với cap 200 entries (~200KB), file I/O là đủ nhanh.
- **Snapshot câu hỏi không cần lưu** — ExamReviewActivity reload từ LocalExamDataSource theo `examId`. Giả định: pack đề local không bị xóa giữa các lần xem lại.
- **selectedAnswersJson trong history** — lưu để "Xem lại" bài cũ load đúng đáp án đã chọn.

---

## [0.6.0] - 2026-04-23 — Client-First ổn định đa thiết bị + Backend tối thiểu

### Tổng quan

Phiên bản này tập trung tối ưu vận hành chi phí thấp:

- Luồng làm bài chuyển sang **local-first thực sự** (thiết bị là trung tâm)
- Backend chỉ giữ vai trò tối thiểu cho **Auth** và **đồng bộ kết quả cuối**
- App chạy ổn trên nhiều thiết bị Android mà không phụ thuộc IP LAN thay đổi

---

### ✨ Android — Thay đổi chính

#### `app/build.gradle.kts`
- [x] Cấu hình mặc định dùng `BASE_URL_CLOUD` cho cả debug/release
- [x] Giữ `LOCAL_LAN_HOST` làm tùy chọn dev nội bộ
- [x] Tắt `USE_LOCAL_BACKEND` mặc định để tránh phụ thuộc IP LAN khi chạy đa thiết bị

#### `ApiClient.java`
- [x] Chuẩn hóa resolve base URL theo BuildConfig
- [x] Thêm log runtime backend URL để debug nhanh trên Logcat
- [x] Thêm hàm `getCurrentBaseUrl()` phục vụ hiển thị runtime status

#### `LoginActivity.java`
- [x] Hiển thị backend URL trong debug build để xác định endpoint thực tế app đang gọi

#### `HomeFragment.java` + `ExamFragment.java`
- [x] Trong chế độ `CLIENT_SIDE_EXAM_PROCESSING=true`, ưu tiên nạp đề từ local datasource
- [x] Giảm phụ thuộc vào backend exam list khi user chỉ cần làm bài trên thiết bị

#### `ExamSessionActivity.java`
- [x] Chạy phiên thi local ngay lập tức (không chặn bởi `sessions/start`)
- [x] Bootstrap session online chạy nền (non-blocking) để đồng bộ kết quả cuối nếu backend sẵn sàng
- [x] Chỉ gửi `client-submit` khi có remote session hợp lệ
- [x] Bổ sung trạng thái sync trực quan trên màn hình thi:
  - `Sync: local-only`
  - `Sync: online enabled`

#### `activity_exam_session.xml`
- [x] Thêm `tvSyncStatus` ngay dưới top bar để thể hiện trạng thái local/online sync realtime

---

### 📚 Local Exam Packs

#### `LocalExamDataSource.java`
- [x] Nâng cấp từ single-file sang multi-pack scan `sample_*.json`
- [x] Tự động nạp toàn bộ đề local trong `assets/`
- [x] Chống trùng ID câu hỏi giữa nhiều đề bằng namespace theo examId
- [x] Fallback an toàn về `sample_math_exam.json` nếu không có file theo pattern

#### Assets mới
- [x] `app/src/main/assets/sample_english_exam.json`
- [x] `app/src/main/assets/sample_physics_exam.json`
- [x] Giữ `sample_math_exam.json` làm bộ đề mẫu nền tảng

---

### 🧭 Tài liệu

#### `README.md`
- [x] Cập nhật kiến trúc client-first theo backend tối thiểu
- [x] Bổ sung API dependency map: endpoint bắt buộc vs tùy chọn vs không bắt buộc
- [x] Bổ sung quy ước mở rộng đề local `sample_*.json`
- [x] Bổ sung mô tả trạng thái sync trong màn hình thi

---

### ✅ Kết quả đạt được

- App có thể vận hành làm bài ổn định theo mô hình local-first
- Giảm rủi ro downtime backend ảnh hưởng trực tiếp trải nghiệm thi
- Dễ mở rộng ngân hàng đề local mà không phải sửa code luồng chính
- Phù hợp chiến lược giảm chi phí server (backend mỏng, chỉ giữ phần cần bảo mật và đồng bộ)

## [0.5.0] - 2026-04-10 — Kiến trúc Hybrid: Xử lý cục bộ + Đồng bộ kết quả

### Tổng quan thay đổi kiến trúc

Chuyển từ hai cực "full server" và "full offline" sang kiến trúc **hybrid** hợp lý hơn:

| Bước | Xử lý | Ghi chú |
|------|--------|---------|
| Đăng nhập / xác thực | **Server** | Kiểm tra tài khoản, quyền mua đề |
| Lấy danh sách đề thi | **Server** | `GET /exams` |
| Bắt đầu phiên thi | **Server** | `POST /sessions/start` — kiểm tra quyền truy cập |
| Tải câu hỏi | **Server** (cache RAM) | Mỗi câu chỉ gọi API 1 lần, sau đó đọc từ `questionCache` |
| Đếm thời gian | **Thiết bị** | `CountDownTimer` |
| Lưu đáp án đã chọn | **Thiết bị** | `selectedAnswers` Map — không gọi API theo từng câu |
| Chấm điểm | **Thiết bị** | So sánh với `option.isCorrect()` từ `questionCache` |
| Nộp kết quả | **Server** (fire-and-forget) | Chỉ POST `{score, correct, total, timeSpent}` |

**Lợi ích:** Giảm API calls từ `1 + N + N + 1` xuống `1 + N(cached) + 1`. Không cần server xử lý scoring. Kết quả được lưu vào DB mà không cần logic phức tạp phía server.

---

### ✨ Android — Thay đổi

#### `ExamApi.java`
- [x] Thêm endpoint `POST /sessions/{sessionId}/client-submit` — nhận kết quả đã tính sẵn từ client

#### `ApiClient.java`
- [x] Cập nhật comment giải thích `CLIENT_SIDE_EXAM_PROCESSING=true` (timer + chấm điểm cục bộ; vẫn cần mạng để auth, fetch đề, ghi kết quả)

#### `AuthRepository.java`
- [x] Xóa early-return offline bypass trong `login()`, `register()`, `getMe()` → tất cả gọi API thực tế
- [x] Giữ `createOfflineAuth()` chỉ làm fallback khi mất mạng

#### `HomeFragment.java`
- [x] Xóa offline shortcut trong `loadUserProfile()` và `loadExams()` → luôn gọi API trước, fallback cục bộ khi lỗi

#### `ExamFragment.java`
- [x] Xóa offline shortcut trong `loadExams()` → gọi `GET /exams`, fallback về `LocalExamDataSource` khi lỗi

#### `ExamSessionActivity.java`
- [x] Thêm `Map<Long, Question> questionCache` — lưu câu hỏi đã fetch để tránh gọi lại + dùng để chấm điểm
- [x] `startSession()`: bỏ offline early-return, luôn gọi `POST /sessions/start`; `sessionStartMillis` set ngay khi bắt đầu
- [x] `loadQuestion()`: phục vụ từ `questionCache` nếu đã có, ngược lại gọi API rồi lưu vào cache
- [x] `submitAnswer()`: vẫn no-op (không gửi từng đáp án lên server) — chỉ lưu vào `selectedAnswers` Map
- [x] `submitExamLocally()`: chấm điểm từ `questionCache` (dùng `option.isCorrect()`), fallback về `LocalExamDataSource` nếu câu chưa cache; sau khi tính xong gọi `submitClientResult()` fire-and-forget để ghi kết quả vào DB

---

### 🚀 Backend — Cloud Deploy Setup (từ phiên trước)

#### `Dockerfile` *(mới)*
- [x] Multi-stage build: `gradle:8.7-jdk17` để build → `eclipse-temurin:17-jre-alpine` để run
- [x] Expose port 8080, `ENTRYPOINT` chạy fat JAR

#### `render.yaml` *(mới)*
- [x] Cấu hình deploy lên Render.com free tier
- [x] Health check path `/api/v1/actuator/health`
- [x] Env vars: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod`

#### `application.yml`
- [x] Profile `prod`: HikariCP pool tối ưu cho Neon free tier (`maximumPoolSize=5`, `minimumIdle=1`, `connectionTimeout=20000`)

---

### ⚠️ Lưu ý triển khai

- Backend chưa implement module **Exam/Session/Question** (chỉ có Auth module). App tự động fallback về `LocalExamDataSource` (`sample_math_exam.json`) khi API chưa có.
- Khi backend đủ module, tắt fallback bằng cách không dùng `LocalExamDataSource` trong các `onFailure`.
- Endpoint `POST /sessions/{id}/client-submit` cần được thêm vào `SessionController` trên backend.

---

## [0.4.0] - 2026-04-08 — Triển khai toàn bộ UI Mockup + Enhanced Screens

### ✨ Tính năng mới — Android UI Implementation

#### Màn hình mới
- [x] **ExamDetailActivity** — Chi tiết đề thi: gradient header, info cards (thời gian, số câu, độ khó), mô tả, nút "Bắt đầu thi ngay"
- [x] **PracticeFragment** — Lộ trình cải thiện: 
  - 2 chủ đề cần cải thiện (Hình học không gian 45%, Logarit & Hàm số mũ 60%) với nút "Luyện tập ngay"
  - 4 chủ đề theo tiến độ (Số học & Đại số 80%, Giải tích 50%, Xác suất & Thống kê 30%, Vật lí hạt nhân 70%) với nút "Tiếp tục"
- [x] **Fragment_practice.xml** — ScrollView + MaterialCardView + custom progress bars

#### Cải thiện HomeFragment
- [x] Gradient header với chào buổi (Morning/Afternoon/Evening)
- [x] Circular score gauge display
- [x] 3 stat cards: Đề làm, Câu đúng, Tỷ lệ thành công
- [x] "Tiếp tục luyện tập" section
- [x] RecyclerView gợi ý ngang (SuggestionAdapter)
- [x] RecyclerView đề gần đây

#### Cải thiện ExamSessionActivity  
- [x] Toolbar: nút back, title, bookmark icon, grid icon
- [x] Progress bar thể hiện tiến độ làm bài
- [x] Timer hiển thị bên cạnh số câu (`45:50` | `Câu 1/50`)
- [x] **Question Grid Dialog**: 7-column grid, màu sắc phân biệt (answered=green, unanswered=gray, current=blue border, bookmarked=orange flag)
- [x] **Bookmark toggle**: lưu câu hỏi yêu thích, icon màu thay đổi theo trạng thái
- [x] Nút "Nộp bài" từ grid dialog

#### Cải thiện ExamResultActivity
- [x] Top bar: nút back, tiêu đề, chỉn chu
- [x] **Circular score chart** (ring progress): 820/1200 điểm V-SAT
- [x] **Thống kê bên phải**: thời gian làm bài, số câu đúng
- [x] **"Kết quả theo môn"** section với progress bars: Toán 90%, Lí 70%
- [x] **"Chủ đề cần cải thiện"** section: Hình học không gian 55%, Dao động cơ 60%, Số phức 65%
- [x] 2 nút hành động: "Xem lời giải chi tiết" (purple), "Luyện tập thêm" (outline)

#### Cải thiện ExamAdapter + item_exam.xml
- [x] Redesign item card: bold title, icon + question count, icon + duration, "Đề miễn phí"/"30.000đ" badge, "Làm bài" button
- [x] Vietnamese text with diacritics: "câu", "phút"
- [x] Better spacing + Material Design 3 styling

#### Cải thiện ExamFragment
- [x] "Kho đề thi" header
- [x] Search bar (MaterialCardView + EditText)
- [x] Horizontal filter chips: Tất cả, Toán, Tiếng Anh, Vật lí, Hóa học
- [x] RecyclerView danh sách đề

#### Bảng điều hướng
- [x] 4 tabs: Trang chủ, Kho đề, Luyện tập, Tài khoản
- [x] Custom vector icons cho mỗi tab
- [x] Primary color gradient

#### Cập nhật ProfileFragment
- [x] Vietnamese diacritics throughout ("Thông tin cá nhân", "Số điện thoại", "Chưa cập nhật", "Đăng xuất")

### 🎨 Design & Colors
- [x] **Primary**: Purple/Indigo theme (#4A3ABA)
- [x] **Secondary**: Accent colors (#FF6C5CE7)
- [x] **Success**: Green (#4CAF50)
- [x] **Warning**: Orange (#FF9800)
- [x] **Error**: Red
- [x] **Drawables created** (20+ files):
  - ic_home, ic_exam, ic_practice, ic_profile (nav icons)
  - ic_back, ic_grid, ic_bookmark, ic_timer, ic_check_circle (feature icons)
  - bg_gradient_header, bg_gradient_card, bg_chip_selected/unselected (backgrounds)
  - bg_button_primary, bg_button_outline (button styles)
  - bg_question_answered, bg_question_unanswered, bg_question_current (question states)
  - circular_progress.xml (ring-shaped score gauge)
  - progress_green.xml, progress_orange.xml, progress_purple.xml (progress bars)

### Mã nguồn — File thay đổi

**Java (6 files)**
- ExamSessionActivity.java: bookmark toggle, grid dialog, Vietnamese text
- ExamResultActivity.java: circular score, V-SAT 1200-point scale
- HomeFragment.java: greeting logic, adapters setup
- ProfileFragment.java: Vietnamese diacritics
- QuestionGridAdapter.java: NEW — 7-column grid, state colors
- SuggestionAdapter.java: NEW (created in 0.3.0, improved)

**Layouts (9 files)**
- activity_exam_detail.xml: gradient header + info cards + button
- activity_exam_session.xml: toolbar with icons, timer, progress
- activity_exam_result.xml: circular score + stats + breakdowns
- fragment_practice.xml: NEW — scrollable topic cards + progress lists
- dialog_question_grid.xml: NEW — grid + stats + submit button
- item_question_grid.xml: NEW — numbered cells with state colors
- item_exam.xml: redesigned card layout
- fragment_exam.xml: search + filter chips
- fragment_profile.xml: Vietnamese labels

**Drawables (20+ files)**
- 10 vector icons for tabs/features
- 10 background drawables for buttons/states
- 3 custom progress bar drawables
- 1 circular progress ring

**Manifest & Config**
- AndroidManifest.xml: + ExamDetailActivity registration
- mobile_navigation.xml: + nav_practice fragment
- bottom_nav_menu.xml: 4 items with icons
- colors.xml: expanded color palette

### Bản dựng
- **API Call**: ExamFragment → ExamDetailActivity → ExamSessionActivity → ExamResultActivity
- **Navigation**: BottomNav 4 tabs + intent-based activities
- **Data Flow**: Retrofit API → RecyclerViews → Activities
- **State Management**: BookmarkedQuestions Set, SelectedAnswers Map persisted during session
- **Build Status**: ✅ BUILD SUCCESSFUL (all tasks up-to-date)

---

## [0.3.0] - 2026-04-04 — Kết nối Neon DB + Fix Auth + GitHub

### Đã hoàn thành
- [x] Kết nối Neon Serverless PostgreSQL (ap-southeast-1)
- [x] Chạy `vsat_database_schema.sql` trên Neon → 27 bảng + 20 ENUM types
- [x] Tạo file `.env` với credentials thực tế
- [x] **DataInitializer**: tự tạo 4 tài khoản test khi backend khởi động
  - `student@vsat.com` / `Student@123` (STUDENT)
  - `collab@vsat.com` / `Admin@123` (COLLABORATOR)
  - `content@vsat.com` / `Admin@123` (CONTENT_ADMIN)
  - `admin@vsat.com` / `Admin@123` (SUPER_ADMIN)

### Fix lỗi Android ↔ Backend JSON mismatch
- [x] **RegisterRequest**: bỏ `@SerializedName("full_name")` → gửi đúng `fullName`
- [x] **AuthResponse**: bỏ `@SerializedName("access_token/refresh_token")` → parse đúng `accessToken`/`refreshToken`
- [x] **UserProfile**: bỏ `@SerializedName("full_name"/"avatar_url")` → parse đúng camelCase từ backend
- [x] **AuthApi logout**: thêm body `{refreshToken: "..."}` theo đúng spec backend
- [x] **ApiClient**: thêm `getRefreshToken()` public method

### GitHub
- [x] Init git repository, push lên `https://github.com/haizzdungnay/VSAT_COMPASS`
- [x] Tạo `README.md` + cập nhật `CHANGELOG.md`

---

## [0.2.0] - 2026-04-03 — Android App MVP

### Android (25 Java files, 22 XML resources)

#### Architecture
- MVVM: ViewModel + LiveData + Repository pattern
- Retrofit 2 + OkHttp3 (JWT interceptor tự động gắn Bearer token)
- Jetpack Navigation Component + BottomNavigationView
- ViewBinding toàn bộ

#### Data Layer
- `ApiClient.java` — Retrofit singleton, JWT interceptor, SharedPreferences token storage
- `AuthApi.java` — Retrofit interface: login, register, refresh, logout, getMe
- `ExamApi.java` — Retrofit interface: danh sách đề, chi tiết, start/submit session, submit answer
- Models: `ApiResponse<T>`, `AuthResponse`, `LoginRequest`, `RegisterRequest`,
  `UserProfile`, `Exam`, `Question`, `ExamSession`
- `AuthRepository.java` — LiveData-based auth operations
- `Resource<T>` — LOADING/SUCCESS/ERROR wrapper

#### UI Layer
- `SplashActivity` — tự redirect dựa trên access token
- `LoginActivity` + `RegisterActivity` — auth screens với validation
- `MainActivity` — BottomNav (3 tabs: Trang chủ, Đề thi, Cá nhân)
- `HomeFragment` — chào mừng + thống kê học viên
- `ExamFragment` — danh sách đề thi (RecyclerView + SwipeRefresh)
- `ExamAdapter` — item card: tiêu đề, số câu, thời gian, nút "Làm bài"
- `ExamSessionActivity` — làm bài thi: timer đếm ngược, điều hướng câu hỏi, lưu đáp án
- `ExamResultActivity` — xem điểm, số câu đúng, thời gian làm bài
- `ProfileFragment` — thông tin cá nhân + đăng xuất

#### Resources
- Layouts: `activity_splash`, `activity_login`, `activity_register`, `activity_main`,
  `activity_exam_session`, `activity_exam_result`, `fragment_home`, `fragment_exam`,
  `fragment_profile`, `item_exam`
- Navigation graph: `mobile_navigation.xml` (3 fragments)
- Bottom nav menu: `bottom_nav_menu.xml`
- Colors: primary blue theme + text/error/success colors

#### Cấu hình
- `AndroidManifest.xml`: INTERNET permission, `usesCleartextTraffic=true`, 6 activities khai báo
- `libs.versions.toml`: toàn bộ dependencies khai báo type-safe
- `app/build.gradle.kts`: viewBinding enabled, Java 11, minSdk 28, targetSdk 36

---

## [0.1.0] - 2026-04-02 — Backend MVP (9 Modules hoàn thành)

### Infrastructure (Module 0)
- [x] VsatCompassApiApplication, SecurityConfig, JpaConfig, OpenApiConfig
- [x] JwtAuthenticationFilter, JwtUtils, CustomUserDetails, CustomUserDetailsService
- [x] AppException (factory methods: badRequest, notFound, forbidden, conflict, unauthorized)
- [x] GlobalExceptionHandler (xử lý tập trung toàn bộ exception)
- [x] SecurityUtils, UserMapper
- [x] AuthController, AuthService, AuthServiceImpl (7 endpoints)
- [x] User, RefreshToken entities + repositories
- [x] 3 Auth enums: UserRole, UserStatus, GenderType
- [x] spring-dotenv: load `.env` tự động

### Module 1 — Subject & Topic
- [x] 3 Entities: Subject, Topic, Subtopic
- [x] SubjectService + SubjectServiceImpl
- [x] SubjectController: 7 endpoints (CRUD subject/topic/subtopic)

### Module 2 — Question Bank
- [x] 4 Entities: Question, QuestionOption, QuestionGroup, QuestionVersion
- [x] 4 Enums: DifficultyLevel, QuestionType, QuestionStatus, ReviewAction
- [x] QuestionService + QuestionServiceImpl (CRUD, filter, options, version++)
- [x] QuestionController (collaborator): 5 endpoints
- [x] QuestionAdminController: 4 endpoints

### Module 3 — Review Workflow
- [x] 2 Entities: QuestionReview, QuestionComment
- [x] ReviewService + ReviewServiceImpl (status transitions: APPROVE/REJECT/REQUEST_REVISION)
- [x] ReviewController: 4 endpoints (create review, list, add/list comments)

### Module 4 — Exam Management
- [x] 2 Entities: Exam, ExamQuestion
- [x] 3 Enums: ExamStatus, ExamPricingType, DifficultyLevel
- [x] ExamService + ExamServiceImpl
- [x] ExamAdminController: 7 endpoints (CRUD + questions + status)
- [x] ExamPublicController: 2 public GET endpoints

### Module 5 — Session Engine
- [x] 2 Entities: ExamSession, SessionAnswer
- [x] 2 Enums: SessionMode, SessionStatus
- [x] SessionService + SessionServiceImpl (start, submit, scoring)
- [x] SessionController: 6 endpoints

### Module 6 — Student Stats
- [x] 1 Entity: UserTopicStats
- [x] StudentStatsService + StudentStatsServiceImpl
- [x] StudentStatsController: 3 endpoints (topic stats, weak topics, exam history)

### Module 7 — Ticket System
- [x] 2 Entities: Ticket, TicketComment
- [x] 2 Enums: TicketType, TicketStatus
- [x] TicketService + TicketServiceImpl (UUID ticketCode, lifecycle, comments)
- [x] TicketController (student): 4 endpoints
- [x] TicketAdminController: 6 endpoints

### Module 8 — Dashboard
- [x] DashboardService + DashboardServiceImpl (aggregate counts)
- [x] DashboardController: 1 endpoint (admin overview)

### Module 9 — User Management
- [x] UserManagementService + UserManagementServiceImpl
- [x] UserManagementController: 4 endpoints (list filter, detail, role, status) — SUPER_ADMIN only

---

## Thống kê tổng

| Hạng mục | Số lượng |
|----------|----------|
| Bảng database | 27 |
| ENUM types PostgreSQL | 20 |
| API endpoints MVP | ~52 |
| Backend Java files | 126+ |
| Android Java files | 25 |
| Android XML resources | 22 |


## [0.1.0] - 2026-04-02 — Khởi tạo dự án & Auth Module

### Đã hoàn thành trước phiên làm việc này
- Phân tích & thiết kế: tài liệu tổng hợp, database schema 27 bảng, 20 ENUM types
- Đặc tả kỹ thuật: 67 màn hình, ma trận quyền 65+ hành động, 79 API endpoints
- Spring Boot Auth module: 7/9 endpoints (register, login, refresh, logout, getMe, updateProfile, changePassword)
- JWT authentication: access token 15 phút + refresh token 30 ngày
- Security config: role-based access control, CORS, BCrypt password encoding
- Base infrastructure: ApiResponse wrapper, AppException, GlobalExceptionHandler
- Swagger/OpenAPI config

### Đã hoàn thành trong phiên làm việc này

#### Backend (Spring Boot 3.2.5)
- [x] Extract project `vsat-compass-api` từ zip archive
- [x] Setup Gradle wrapper (Gradle 8.7) cho backend project
- [x] Cấu hình `application.yml` với dev/prod profiles, Neon PostgreSQL

#### Thiết kế chi tiết cho các module tiếp theo
- [x] Module 1 — Subject & Topic: 3 entities, 7 API endpoints (SB-01 đến SB-07)
- [x] Module 2 — Question Bank: 4 entities, 3 enums, 6 API endpoints (QS-01 đến QS-05)
- [x] Module 3 — Review Workflow: 2 entities, 1 enum, 4 API endpoints (RV-01 đến RV-04)
- [x] Module 4 — Exam Management: 2 entities, 2 enums, 9 API endpoints (EX-01 đến EX-09)
- [x] Module 5 — Exam Session Engine: 2 entities, 2 enums, 7 API endpoints (ES-01 đến ES-07)
- [x] Module 6 — History & Analytics: 1 entity, 2 API endpoints
- [x] Module 7 — Ticket System: 2 entities, 2 enums, 7 API endpoints (TK-01 đến TK-07)
- [x] Module 8 — Dashboard: 2 API endpoints (admin + student)
- [x] Module 9 — User Management: 4 API endpoints (UM-01 đến UM-04)
- [x] Android App: thiết kế MVVM architecture, package structure, auth screens

### Đang triển khai (in progress)
- [ ] Code Android app (Auth, Student, Admin screens)
- [ ] Cấu hình database credentials (.env)
- [ ] Build & test backend

### Hoàn thành Backend Modules 1-9 (126 Java files)

#### Module 0 — Infrastructure (từ zip gốc)
- [x] VsatCompassApiApplication, SecurityConfig, JpaConfig, OpenApiConfig
- [x] JwtAuthenticationFilter, JwtUtils, CustomUserDetails, CustomUserDetailsService
- [x] AppException, GlobalExceptionHandler, SecurityUtils, UserMapper
- [x] AuthController, AuthService, AuthServiceImpl
- [x] User, RefreshToken entities + repositories
- [x] 3 Auth enums: UserRole, UserStatus, GenderType

#### Module 1 — Subject & Topic (hoàn thành)
- [x] 3 Entities: Subject, Topic, Subtopic
- [x] 3 Repositories, 3 Request DTOs, 4 Response DTOs
- [x] SubjectService + SubjectServiceImpl
- [x] SubjectController (7 endpoints: CRUD subjects/topics/subtopics)

#### Module 2 — Question Bank (hoàn thành)
- [x] 4 Entities: Question, QuestionOption, QuestionGroup, QuestionVersion
- [x] 4 Repositories, 1 Request DTO (inner classes), 2 Response DTOs
- [x] QuestionService + QuestionServiceImpl
- [x] QuestionController (collaborator: 5 endpoints)
- [x] QuestionAdminController (admin: 4 endpoints)
- [x] 4 Enums: DifficultyLevel, QuestionType, QuestionStatus, ReviewAction

#### Module 3 — Review Workflow (hoàn thành)
- [x] 2 Entities: QuestionReview, QuestionComment
- [x] 2 Repositories, 1 Request DTO, 2 Response DTOs
- [x] ReviewService + ReviewServiceImpl
- [x] ReviewController (4 endpoints: create review, list reviews, add/list comments)

#### Module 4 — Exam Management (hoàn thành)
- [x] 2 Entities: Exam, ExamQuestion
- [x] 2 Repositories, 1 Request DTO (inner classes), 3 Response DTOs
- [x] ExamService + ExamServiceImpl
- [x] ExamAdminController (7 endpoints: CRUD + status + questions)
- [x] ExamPublicController (2 public GET endpoints)
- [x] 3 Enums: ExamStatus, ExamPricingType, DifficultyLevel

#### Module 5 — Session Engine (hoàn thành)
- [x] 2 Entities: ExamSession, SessionAnswer
- [x] 2 Repositories, 2 Request DTOs, 3 Response DTOs
- [x] SessionService + SessionServiceImpl
- [x] SessionController (6 endpoints: start, submit, save answer, get session/answers)
- [x] 2 Enums: SessionMode, SessionStatus

#### Module 6 — Student Stats (hoàn thành)
- [x] 1 Entity: UserTopicStats
- [x] 1 Repository, 2 Response DTOs
- [x] StudentStatsService + StudentStatsServiceImpl
- [x] StudentStatsController (3 endpoints: topic stats, weak topics, exam history)

#### Module 7 — Ticket System (hoàn thành)
- [x] 2 Entities: Ticket, TicketComment
- [x] 2 Repositories, 1 Request DTO, 2 Response DTOs
- [x] TicketService + TicketServiceImpl
- [x] TicketController (student: 4 endpoints)
- [x] TicketAdminController (admin: 6 endpoints)
- [x] 2 Enums: TicketType, TicketStatus

#### Module 8 — Dashboard (hoàn thành)
- [x] 1 Response DTO (DashboardResponse)
- [x] DashboardService + DashboardServiceImpl
- [x] DashboardController (1 endpoint: admin dashboard overview)

#### Module 9 — User Management (hoàn thành)
- [x] 1 Request DTO, 1 Response DTO
- [x] UserManagementService + UserManagementServiceImpl
- [x] UserManagementController (4 endpoints: list, detail, update role/status)

---

## Cấu trúc project hiện tại

```
VSAT_COMPASS/                    ← Android App (Java + XML)
├── app/src/main/java/com/example/v_sat_compass/
├── build.gradle.kts
├── CHANGELOG.md                 ← File này
└── VSAT/                        ← Tài liệu & tài nguyên
    ├── vsat_database_schema.sql
    ├── vsat_schema_documentation.txt
    └── ui/
        ├── VSAT_COMPASS_BAO_CAO_VA_HUONG_DAN.txt
        ├── tong_hop_du_an_vsat_android_web_v2.txt
        ├── vsat_compass_api_spec.xlsx
        ├── vsat_compass_man_hinh_va_phan_quyen.xlsx
        └── sample/vsat-compass-api.zip

vsat-compass-api/                ← Backend Spring Boot (126 files)
├── build.gradle
├── gradlew / gradlew.bat
├── gradle/wrapper/
└── src/main/java/com/vsatcompass/api/
    ├── config/              (SecurityConfig, OpenApiConfig, JpaConfig)
    ├── security/            (JWT utils, filter, CustomUserDetails)
    ├── entity/              (16 entities + 13 enums)
    ├── repository/          (16 repositories)
    ├── dto/request/         (9 request DTOs)
    ├── dto/response/        (19 response DTOs)
    ├── dto/common/          (ApiResponse)
    ├── service/             (9 service interfaces)
    ├── service/impl/        (9 service implementations)
    ├── controller/auth/     (AuthController)
    ├── controller/admin/    (6 admin controllers)
    ├── controller/collaborator/ (2 collaborator controllers)
    ├── controller/student/  (4 student controllers)
    ├── exception/           (AppException, GlobalExceptionHandler)
    ├── mapper/              (UserMapper)
    └── util/                (SecurityUtils)
```

## Thống kê

| Hạng mục | Số lượng |
|----------|----------|
| Bảng database | 27 |
| ENUM types | 20 |
| API endpoints (thiết kế) | 79 |
| API endpoints (đã code) | ~52 MVP endpoints |
| Backend Java files (hiện có) | 126 |
| Backend modules cần build | 0 (9/9 hoàn thành) |
| Android screens (thiết kế) | 67 |
| Android screens (đã code) | 0 |
