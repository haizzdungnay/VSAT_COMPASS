# Draft Cleanup

Implementation status: implemented for backend DRAFT hard delete in C1.2c.1. Release tag `v0.9.4` points to release commit `c4c2993deba664883132edf043401e41ffdbca61`.

## Problem Statement

DRAFT exams need an explicit cleanup path so admins can remove abandoned work without accidentally erasing published history, audit context, or exam-question composition data. The product needs to decide whether cleanup means delete, archive, or discard before any endpoint or persistence change is made.

## Current State

DRAFT is a normal exam status. Published public APIs only expose PUBLISHED free exams, while admin workflows can create and compose draft exams before publication.

C1.2b-3 did not implement draft deletion, archival, or discard. C1.2c.1 implemented the backend discard endpoint as `DELETE /admin/exams/{examId}`.

The implemented behavior is hard delete for `DRAFT` exams only. Non-DRAFT exams are rejected with `409 INVALID_STATE`, and missing exams return `404 RESOURCE_NOT_FOUND`. The endpoint relies on the existing persistence behavior for `exam_questions` mappings and does not delete `Question` entities.

Audit logging remains deferred. There was no existing production audit service/repository pattern to reuse in C1.2c.1, so the endpoint does not create a new audit framework.

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

Prefer explicit "discard draft" semantics over overloading archive, unless the product vocabulary requires archive for all cleanup operations.

C1.2c.1 selected this direction for the backend API: `DELETE /admin/exams/{examId}` is the discard action, and its persistence behavior is DRAFT-only hard delete. Future work should keep archive semantics separate unless product vocabulary changes.

## Trade-offs

Hard delete is operationally clean but has the highest accidental-loss risk. Archive is safer for data retention but may confuse users if archived drafts appear alongside retired published exams. A discard action adds design and implementation work, but gives the clearest policy boundary for DRAFT-only cleanup.

## Open Questions

- Should discarded drafts ever be recoverable by admins, or is permanent DRAFT hard delete sufficient?
- Should future UI require a confirmation dialog, typed confirmation, or reason before calling discard?
- What audit event should be recorded for discard, and where should it live once an audit framework exists?
- Should future product policy restrict discard to the creator, while C1.2c.1 allows existing admin exam roles?
- Should cleanup be blocked once a draft has ever been published or reviewed? C1.2c.1 only permits current `DRAFT` status.

## Implementation Deferred

C1.2b-3 deferred implementation. C1.2c.1 implemented the backend endpoint and DRAFT hard-delete behavior.

Remaining deferred work:
- Audit logging for discard once a production audit service/repository pattern exists.
- Frontend/admin UI confirmation behavior before calling the endpoint.
- Recovery/undo policy, if the product later needs one.
- Any broader cleanup policy for non-DRAFT statuses.

## Non-Goals for C1.2b-3

- No Java source changes.
- No schema or migration changes.
- No enum changes.
- No delete, archive, or discard endpoint.
- No change to `exam_questions` persistence behavior.
- No change to audit storage or retention behavior.

These non-goals describe C1.2b-3 only. C1.2c.1 later implemented the backend discard endpoint without schema, enum, or audit storage changes. LOCKED semantics and Exam.version behavior were not implemented by this feature.
