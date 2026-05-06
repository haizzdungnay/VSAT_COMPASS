# Locked Semantics

## Problem Statement

LOCKED exists in the exam status vocabulary, but the product has not selected what it means operationally. Introducing transitions too early could conflict with publishing, hiding, archiving, active sessions, or admin collaboration rules.

## Current State

The ExamStatus enum includes LOCKED and marks it as reserved for future use. Current business logic does not use LOCKED as an active workflow state. Existing logic should continue to reject or avoid unsupported LOCKED transitions defensively until a concrete use case is selected.

## Options

### Option A — Lock while active sessions exist

Move an exam into LOCKED while learners have active sessions or while attempts are being finalized.

This protects session consistency and prevents composition or visibility changes while attempts depend on the current exam shape. It requires clear entry and exit rules, a definition of active session, and a recovery path if sessions are abandoned.

### Option B — Lock after publish to prevent composition edits

Treat publication as an immutability boundary and enter LOCKED after PUBLISHED composition is fixed.

This makes the published version stable and prevents accidental edits after release. It overlaps with PUBLISHED semantics, so the design must decide whether LOCKED is a separate status, a flag, or a transition guard around PUBLISHED/HIDDEN/ARCHIVED states.

### Option C — Lock for concurrent admin editing or moderation

Use LOCKED as a short-lived moderation or editing lock.

This can prevent conflicting admin edits and support review workflows. It requires ownership, timeout, unlock permission, and stale-lock handling. It may be better represented by a separate lock record rather than the main exam status.

## Recommendation

Do not introduce LOCKED transitions until a concrete use case is selected. Continue defensive rejection of LOCKED in existing logic and treat it as reserved until C1.2c/C1.3 design clarifies whether the state protects active sessions, published composition, or concurrent admin work.

## Trade-offs

Using LOCKED as a main status is visible and easy to filter, but it may collide with lifecycle statuses such as PUBLISHED, HIDDEN, and ARCHIVED. Using a separate lock concept adds data-model work but avoids making one status carry several meanings. Keeping LOCKED reserved avoids premature behavior but leaves current concurrency and immutability questions unresolved.

## Open Questions

- When exactly should an exam enter LOCKED?
- Is LOCKED terminal, reversible, or time-limited?
- Who can unlock: creator, reviewer, admin, or super admin?
- Can PUBLISHED, HIDDEN, or ARCHIVED exams also be locked, or is LOCKED mutually exclusive?
- Should LOCKED block public visibility, composition edits, publishing, hiding, archiving, or all of these?
- Should lock ownership and expiry be tracked separately from exam status?

## Implementation Deferred

No LOCKED transition, unlock behavior, endpoint, schema change, enum change, or service change is implemented in C1.2b-3. Future implementation should follow a selected use case and include tests for lifecycle interactions.

## Non-Goals for C1.2b-3

- No Java source changes.
- No schema or migration changes.
- No enum changes.
- No LOCKED transition rules.
- No unlock endpoint or permission model.
- No change to PUBLISHED, HIDDEN, or ARCHIVED behavior.
