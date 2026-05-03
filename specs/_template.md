# <Feature Name>

> Status: draft | in-review | approved | implemented
> Owner: <name>
> Last updated: YYYY-MM-DD

## Overview

A short paragraph (3–6 sentences) describing what this feature is, who it is for, and why it matters. State the problem being solved and the desired outcome. Link any related specs, tickets, or design docs.

## User Stories

List the user-facing scenarios this feature must support. Use the standard form:

- As a **<role>**, I want to **<action>**, so that **<benefit>**.
- As a **<role>**, I want to **<action>**, so that **<benefit>**.

## API Contract

Document every endpoint introduced or changed.

### `METHOD /path`

- **Description:** one-line summary.
- **Auth:** required role / scope, or `none`.
- **Path params:** `name` (type) — description.
- **Query params:** `name` (type, required?) — description.
- **Request body:**

```json
{
  "field": "type — description"
}
```

- **Response 2xx:**

```json
{
  "field": "type — description"
}
```

- **Response errors:** list status codes and when each is returned (cross-reference Error Cases).

Repeat the block above for each endpoint.

## Data Model

Describe entities, fields, types, constraints, and relationships. Prefer a table per entity.

### `EntityName`

| Field      | Type           | Constraints                   | Notes |
|------------|----------------|-------------------------------|-------|
| id         | UUID           | PK, not null                  |       |
| name       | varchar(255)   | not null, unique              |       |
| created_at | timestamptz    | not null, default now()       |       |

Note any indexes, foreign keys, or migration considerations.

## Validation Rules

Enumerate every input validation rule. Be specific — bounds, formats, allowed values.

- `field`: required, length 1–255, must match `<regex or rule>`.
- `field`: optional; if present, must be one of `[A, B, C]`.
- Cross-field rules: e.g. `endDate` must be `>= startDate`.

## Error Cases

List every error condition the feature can produce and how it is surfaced.

| Condition                     | HTTP status | Error code         | Message                          |
|-------------------------------|-------------|--------------------|----------------------------------|
| Resource not found            | 404         | `RESOURCE_NOT_FOUND` | "<resource> with id <id> not found" |
| Validation failure            | 400         | `VALIDATION_ERROR` | field-level details              |
| Conflict (duplicate)          | 409         | `CONFLICT`         | "<resource> already exists"      |

## Acceptance Criteria

Concrete, testable conditions that must all hold for the feature to be considered done. Each criterion should map to at least one test.

- [ ] Given <precondition>, when <action>, then <observable outcome>.
- [ ] <criterion>
- [ ] <criterion>

## Out of Scope

Things explicitly **not** included in this spec, to prevent scope creep. State why each item is excluded and where (if anywhere) it will be tracked.

- <item> — deferred to <link/ticket>.
- <item> — not required for this milestone.
