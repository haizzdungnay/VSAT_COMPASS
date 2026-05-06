# Draft Cleanup

## Problem Statement

DRAFT exams need an explicit cleanup path so admins can remove abandoned work without accidentally erasing published history, audit context, or exam-question composition data. The product needs to decide whether cleanup means delete, archive, or discard before any endpoint or persistence change is made.

## Current State

DRAFT is a normal exam status. Published public APIs only expose PUBLISHED free exams, while admin workflows can create and compose draft exams before publication.

There is no C1.2b-3 implementation for draft deletion, archival, or discard. Any future cleanup must account for `exam_questions` mappings because a DRAFT can already have a composition attached. Cleanup also needs an audit story: deleting a row removes evidence of who created or edited the draft, while status-based cleanup keeps more traceability.

## Options

### Option A — Hard-delete DRAFT exams

Allow admins to delete DRAFT exams directly.

This is simple for abandoned drafts and keeps admin lists clean, but it is the riskiest option. Accidental deletion may remove the exam row and associated `exam_questions` mappings. If audit data is only stored on the exam row, hard delete also weakens traceability.

### Option B — Archive DRAFT exams

Move DRAFT exams to ARCHIVED instead of deleting them.

This preserves the exam row and composition mappings, and it fits existing status-based filtering. The downside is semantic overload: ARCHIVED may already imply a previously published or retired exam, while abandoned drafts are different operationally and may need different restore/audit behavior.

### Option C — Add explicit discard-draft semantics

Introduce a dedicated discard action for DRAFT exams.

This keeps the product vocabulary precise. A future endpoint can enforce that only DRAFT exams are discardable, decide whether to hard-delete or status-mark internally, and attach audit semantics intentionally. It also avoids stretching archive semantics unless the product explicitly wants all cleanup to be called archive.

## Recommendation

Prefer explicit "discard draft" semantics over overloading archive, unless the product vocabulary requires archive for all cleanup operations. The implementation should stay deferred until a feature phase can define the exact endpoint, authorization, persistence behavior, and audit contract.

## Trade-offs

Hard delete is operationally clean but has the highest accidental-loss risk. Archive is safer for data retention but may confuse users if archived drafts appear alongside retired published exams. A discard action adds design and implementation work, but gives the clearest policy boundary for DRAFT-only cleanup.

## Open Questions

- Should discarded drafts be recoverable by admins?
- Should draft cleanup preserve `exam_questions` rows, delete them, or rely on cascade behavior?
- What audit event should be recorded for discard, and where should it live?
- Should only the creator, any admin, or only super admin be allowed to discard drafts?
- Should cleanup be blocked once a draft has ever been published or reviewed?

## Implementation Deferred

No endpoint, service logic, repository behavior, schema migration, enum change, or UI behavior is implemented in C1.2b-3. The selected cleanup model should be implemented only after the product and audit semantics are approved.

## Non-Goals for C1.2b-3

- No Java source changes.
- No schema or migration changes.
- No enum changes.
- No delete, archive, or discard endpoint.
- No change to `exam_questions` persistence behavior.
- No change to audit storage or retention behavior.
