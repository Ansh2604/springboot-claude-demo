---
name: openapi-docs
description: Use when adding or updating OpenAPI / Swagger documentation on Spring Boot controllers. Triggers on "document the endpoint", "add OpenAPI annotations", or new @RestController methods.
---

# OpenAPI Docs Skill

## Required annotations
Every public controller method must carry full springdoc-openapi annotations:

- `@Operation(summary = "...", description = "...")` — short, action-oriented summary.
- `@ApiResponses` listing every status the endpoint can return:
  - 2xx success with `@Content(schema = @Schema(implementation = ResponseDto.class))`
  - 4xx client errors (400 validation, 404 not found, 409 conflict) referencing the shared error DTO
  - 5xx only when the controller can deliberately produce it
- `@Parameter(description = "...", example = "...")` on every `@PathVariable` and `@RequestParam`.
- Request bodies: `@RequestBody(required = true, content = @Content(schema = @Schema(implementation = RequestDto.class)))`.
- Group endpoints with `@Tag(name = "...", description = "...")` at the controller class level.

## DTO schemas
- Annotate record components with `@Schema(description = "...", example = "...")`.
- Mark required fields explicitly via `@Schema(requiredMode = REQUIRED)` — don't rely on validation annotations alone.
- Enums: include `@Schema(allowableValues = {...})` or let springdoc derive them.

## Conventions
- Keep summaries under 80 chars; put detail in `description`.
- Examples must be realistic (valid UUIDs, ISO-8601 timestamps) — they appear in generated docs.
- Verify `/v3/api-docs` and `/swagger-ui.html` render the new endpoint after changes.
