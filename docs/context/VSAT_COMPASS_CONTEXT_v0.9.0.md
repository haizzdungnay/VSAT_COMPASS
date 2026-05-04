# V-SAT Compass Context Handoff — v0.9.0

## Release State

- Current version: `v0.9.0`
- Production URL: `https://vsat-compass-api.onrender.com/api/v1/`
- Deployed code commit: `752c15e76d67cefe7c11be4a07d585a092d6c704`
- Docs closeout commit: `78b0829558e96886c3b9400ffdc0eec1576392a2`
- Latest tag: `v0.9.0`
- Tag points to deployed code commit: yes

## Phase Status

- Phase A: complete
- Phase B: complete
- Phase C: in progress

Completed Phase C items:
- C1.0 Subject + Topic foundation
- C1.1a Question Bank schema foundation
- C1.1b Question CRUD workflow + role-based authorization

## Live Production Modules

- Auth
- Sessions
- Subjects / Topics / Subtopics
- Collaborator Question CRUD
- Admin Question Review workflow

## Production Smoke Status

- `smoke_auth.sh`: 9/9 PASS earlier in same release run; later register-only rerun hit expected 429 rate limit.
- `smoke_sessions.sh`: 5/5 PASS
- `smoke_subjects.sh`: 4/4 PASS
- `smoke_questions.sh`: 32/32 PASS
- 5-minute stability watch: PASS

## Required Smoke Accounts

- `qbank-collab1@vsat.com` → `COLLABORATOR`
- `qbank-collab2@vsat.com` → `COLLABORATOR`
- `qbank-admin@vsat.com` → `CONTENT_ADMIN`

Do not write smoke account passwords into repository files.

## Production Taxonomy Seed

- `MATH` subject exists
- `MATH_SMOKE_ALGEBRA` topic exists
- `MATH_SMOKE_LINEAR` subtopic exists

## Important Rules

- Never add `Co-Authored-By`.
- Never force-push `main`.
- Never modify frozen schema: `VSAT/vsat_database_schema.sql`.
- Keep `backup-pre-batch-2b-20260430` until 2026-06-01.
- Tag release commits, not docs closeout commits.
- Do not commit local JDK/Pleiades changes.

## Local State Note

- `stash@{0}: local jdk25 config -- restore after C1.1b` remains untouched.

## Deferred Next Work

- `question_versions`
- `question_groups`
- exam composition/publication
- Excel import
- Android/admin UI integration
- optional no-register mode for `smoke_auth.sh`
- optional cleanup of old local JDK/Pleiades config after confirming build environment
