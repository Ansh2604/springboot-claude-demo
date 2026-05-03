---
name: flyway-migration
description: Use when authoring or modifying Flyway database migrations. Triggers on "add a migration", "alter table", "create table", or changes under src/main/resources/db/migration.
---

# Flyway Migration Skill

## Conventions
- Migrations live in `src/main/resources/db/migration`.
- File name: `V{N}__{snake_case_description}.sql` (e.g. `V3__add_due_date_to_tasks.sql`). Increment `N` monotonically — never reuse or reorder applied versions.
- One logical change per migration. Keep it forward-only; do not edit a migration that has been applied to any environment.

## Authoring rules
- Use Postgres SQL. Always specify column types explicitly (`UUID`, `TIMESTAMP WITH TIME ZONE`, `TEXT`, `BOOLEAN`).
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` for new tables (requires `pgcrypto`).
- Audit columns: `created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()`, `updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()`.
- Add `NOT NULL` columns to existing tables in two steps when the table has data: (1) add nullable + backfill, (2) set `NOT NULL` in a follow-up migration.
- Index foreign keys and frequently filtered columns. Name indexes `idx_{table}_{column}`.
- Foreign keys: `CONSTRAINT fk_{table}_{ref_table} FOREIGN KEY (...) REFERENCES ...`.

## Verification
- Run `mvn verify` — Testcontainers will execute migrations end-to-end against a real Postgres.
- Confirm the JPA entity matches the migrated schema (column names, nullability, types).
