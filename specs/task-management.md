# Task Management

> Status: draft
> Owner: task-manager team
> Last updated: 2026-05-03

## Overview

The Task Management feature provides CRUD operations for individual tasks tracked by the `task-manager` service. It exposes a REST API under `/api/v1/tasks` for creating, reading, updating, deleting, listing (with pagination), and filtering tasks by status and priority. This is the foundational feature of the service — all higher-level workflows (assignment, reporting, notifications) build on the entity and endpoints defined here.

## User Stories

- As an **API consumer**, I want to **create a task with a title, description, status, priority, and due date**, so that **I can track work that needs to be done**.
- As an **API consumer**, I want to **fetch a single task by id**, so that **I can display its current state**.
- As an **API consumer**, I want to **update a task's fields**, so that **I can reflect progress and changing requirements**.
- As an **API consumer**, I want to **delete a task**, so that **I can remove work that is no longer relevant**.
- As an **API consumer**, I want to **list tasks with pagination**, so that **I can browse large task sets without loading them all at once**.
- As an **API consumer**, I want to **filter the task list by status and/or priority**, so that **I can focus on a relevant subset (e.g. open high-priority tasks)**.

## API Contract

All endpoints are rooted at `/api/v1/tasks` and produce/consume `application/json`.

### `POST /api/v1/tasks`

- **Description:** Create a new task.
- **Auth:** none (TBD — see Out of Scope).
- **Request body:**

```json
{
  "title": "string — required, ≤ 200 chars",
  "description": "string — optional, ≤ 2000 chars",
  "status": "TODO | IN_PROGRESS | DONE — optional, defaults to TODO",
  "priority": "LOW | MEDIUM | HIGH — optional, defaults to MEDIUM",
  "dueDate": "ISO-8601 date (YYYY-MM-DD) — optional"
}
```

- **Response 201 Created:** full task representation (see schema below). `Location` header points at `/api/v1/tasks/{id}`.
- **Response errors:** `400` on validation failure.

### `GET /api/v1/tasks/{id}`

- **Description:** Fetch a single task.
- **Auth:** none.
- **Path params:** `id` (UUID) — task identifier.
- **Response 200 OK:** task representation.
- **Response errors:** `404` if no task with that id.

### `PUT /api/v1/tasks/{id}`

- **Description:** Replace a task's mutable fields.
- **Auth:** none.
- **Path params:** `id` (UUID).
- **Request body:** same shape as `POST`. `id`, `createdAt`, and `updatedAt` are server-managed and rejected if supplied.
- **Response 200 OK:** updated task representation.
- **Response errors:** `400` on validation failure, `404` if id not found.

### `DELETE /api/v1/tasks/{id}`

- **Description:** Delete a task.
- **Auth:** none.
- **Path params:** `id` (UUID).
- **Response 204 No Content** on success.
- **Response errors:** `404` if id not found.

### `GET /api/v1/tasks`

- **Description:** List tasks with pagination and optional filtering.
- **Auth:** none.
- **Query params:**
  - `page` (int, optional, default `0`) — zero-based page index.
  - `size` (int, optional, default `20`, max `100`) — page size; values above `100` are clamped to `100`.
  - `sort` (string, optional, default `createdAt,desc`) — Spring Data sort spec.
  - `status` (enum, optional) — one of `TODO`, `IN_PROGRESS`, `DONE`.
  - `priority` (enum, optional) — one of `LOW`, `MEDIUM`, `HIGH`.
- **Response 200 OK:**

```json
{
  "content": [ /* task representations */ ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7
}
```

- **Response errors:** `400` if `status` or `priority` is not a valid enum value, or if `page`/`size` are negative.

### Task representation

```json
{
  "id": "UUID",
  "title": "string",
  "description": "string | null",
  "status": "TODO | IN_PROGRESS | DONE",
  "priority": "LOW | MEDIUM | HIGH",
  "dueDate": "YYYY-MM-DD | null",
  "createdAt": "ISO-8601 timestamp (UTC)",
  "updatedAt": "ISO-8601 timestamp (UTC)"
}
```

## Data Model

### `Task`

| Field       | Type                        | Constraints                                       | Notes |
|-------------|-----------------------------|---------------------------------------------------|-------|
| id          | UUID                        | PK, not null, server-generated                    | |
| title       | varchar(200)                | not null, length 1–200                            | |
| description | varchar(2000)               | nullable, length ≤ 2000                           | |
| status      | varchar(16) (enum)          | not null, in `{TODO, IN_PROGRESS, DONE}`, default `TODO` | stored as string for readability |
| priority    | varchar(8) (enum)           | not null, in `{LOW, MEDIUM, HIGH}`, default `MEDIUM` | stored as string |
| due_date    | date                        | nullable                                          | |
| created_at  | timestamptz                 | not null, set on insert                           | server-managed |
| updated_at  | timestamptz                 | not null, set on insert and every update          | server-managed |

Indexes:

- PK on `id`.
- Index on `status` (filter).
- Index on `priority` (filter).
- Composite index on `(status, priority)` to support combined filtering.

## Validation Rules

- `title`: required, length 1–200, trimmed; blank string after trim is rejected.
- `description`: optional; if present, length ≤ 2000.
- `status`: optional on create (defaults to `TODO`); if present, must be one of `TODO`, `IN_PROGRESS`, `DONE`.
- `priority`: optional on create (defaults to `MEDIUM`); if present, must be one of `LOW`, `MEDIUM`, `HIGH`.
- `dueDate`: optional; must be a valid ISO-8601 date (`YYYY-MM-DD`).
- `id`, `createdAt`, `updatedAt` in request bodies: rejected — these are server-managed.
- Query params: `page` ≥ 0; `size` in `[1, 100]` (values > 100 are clamped to 100); `status` and `priority` must match the enum sets above.

## Error Cases

| Condition                          | HTTP status | Error code           | Message                                              |
|------------------------------------|-------------|----------------------|------------------------------------------------------|
| Required field missing / blank     | 400         | `VALIDATION_ERROR`   | field-level details (e.g. `title: must not be blank`) |
| Field exceeds length limit         | 400         | `VALIDATION_ERROR`   | `<field>: length must be ≤ <max>`                    |
| Invalid enum value (`status`/`priority`) | 400   | `VALIDATION_ERROR`   | `<field>: must be one of [...]`                      |
| Invalid date format (`dueDate`)    | 400         | `VALIDATION_ERROR`   | `dueDate: must be ISO-8601 date (YYYY-MM-DD)`        |
| Negative `page` or out-of-range `size` | 400     | `VALIDATION_ERROR`   | parameter-level details                              |
| Task id not found                  | 404         | `TASK_NOT_FOUND`     | `Task with id <id> not found`                        |
| Malformed UUID in path             | 400         | `VALIDATION_ERROR`   | `id: must be a valid UUID`                           |

All errors are produced by the global `@RestControllerAdvice` handler and follow the service's standard error envelope.

## Acceptance Criteria

- [ ] `POST /api/v1/tasks` with a valid body returns `201`, a `Location` header, and the persisted representation including `id`, `createdAt`, `updatedAt`.
- [ ] `POST /api/v1/tasks` with a missing or blank `title` returns `400` with a `VALIDATION_ERROR` describing the offending field.
- [ ] `POST /api/v1/tasks` with `title` longer than 200 chars or `description` longer than 2000 chars returns `400`.
- [ ] `POST /api/v1/tasks` without `status` or `priority` defaults them to `TODO` and `MEDIUM`.
- [ ] `GET /api/v1/tasks/{id}` returns `200` and the task for an existing id, `404` for an unknown id, `400` for a malformed UUID.
- [ ] `PUT /api/v1/tasks/{id}` updates mutable fields, refreshes `updatedAt`, and rejects attempts to set `id`/`createdAt`/`updatedAt`.
- [ ] `DELETE /api/v1/tasks/{id}` returns `204` and the task is no longer retrievable; deleting an unknown id returns `404`.
- [ ] `GET /api/v1/tasks` paginates with `page=0&size=20` by default, sorted by `createdAt` desc.
- [ ] `GET /api/v1/tasks?size=500` clamps page size to 100.
- [ ] `GET /api/v1/tasks?status=TODO` returns only tasks with status `TODO`; same for `priority`.
- [ ] `GET /api/v1/tasks?status=TODO&priority=HIGH` returns only tasks matching both filters.
- [ ] Each endpoint has an integration test covering its golden path and at least one error case.
- [ ] OpenAPI documentation is auto-generated by SpringDoc and is reachable at `/swagger-ui.html` and `/v3/api-docs`, with all endpoints, request/response schemas, and enum values represented.

## Out of Scope

- Authentication and authorization — all endpoints are anonymous in this milestone; auth will be tracked separately.
- Task assignment to users (no `assigneeId` field yet).
- Soft delete / archival — `DELETE` is hard-delete only.
- Bulk create / update / delete endpoints.
- Full-text search across `title` / `description`.
- Audit log of changes beyond `createdAt` / `updatedAt`.
- Real-time notifications or webhooks on task changes.
