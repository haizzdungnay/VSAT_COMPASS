# Exam Version

## Problem Statement

Exam.version needs a stable meaning before it is used for concurrency control, display, revision tracking, or publish history. Changing its semantics abruptly could break DTO contracts, admin expectations, and future migration planning.

## Current State

Exam.version is currently mapped as a regular integer column and exposed through admin DTOs. It is not annotated as JPA `@Version`, so it does not currently provide optimistic locking. Public exam DTOs intentionally avoid exposing internal version metadata.

## Options

### Option A — Keep version as a business revision field

Continue treating Exam.version as a business/display/revision value.

This preserves current semantics and avoids migration risk. It can support admin-facing labels such as draft revision or publish revision, but it does not solve concurrent update conflicts by itself.

### Option B — Convert version to JPA @Version optimistic locking

Change Exam.version into the persistence-layer optimistic lock field.

This uses standard JPA behavior and can detect conflicting writes. The downside is semantic churn: a field already exposed in admin DTOs would start changing according to persistence flush behavior, not product revision rules. It also requires migration, DTO review, and conflict handling design.

### Option C — Add separate lock_version for optimistic locking

Keep Exam.version as the business field and add a future `lock_version` column for JPA optimistic locking if needed.

This avoids changing the meaning of the existing field. It gives the database and API two explicit concepts: product revision and write-conflict version. The trade-off is an additional column, migration, and mapping work when optimistic locking becomes necessary.

## Recommendation

Prefer Option C if optimistic locking becomes necessary: add a separate `lock_version` later rather than changing the semantic meaning of Exam.version abruptly. No optimistic-locking implementation should be introduced in this phase.

## Trade-offs

Option A is stable but does not prevent lost updates. Option B is compact but risks surprising API and admin behavior because persistence increments become visible as business version changes. Option C is more explicit and safer semantically, but requires a migration and clear conflict-response contract.

## Open Questions

- Should Exam.version represent draft revision, publish revision, or display-only metadata?
- Should admin DTOs expose persistence conflict versions at all?
- What HTTP status and error code should represent optimistic-lock conflicts?
- Should conflict detection apply to composition edits, status transitions, metadata edits, or all admin writes?
- Would question versioning and exam versioning need aligned semantics?

## Implementation Deferred

No JPA `@Version`, `lock_version` column, migration, DTO change, endpoint change, or conflict handling is implemented in C1.2b-3. Future implementation should include migration planning and tests for concurrent admin updates.

## Non-Goals for C1.2b-3

- No Java source changes.
- No schema or migration changes.
- No enum changes.
- No optimistic-locking behavior.
- No DTO or API contract changes.
- No change to current Exam.version semantics.
