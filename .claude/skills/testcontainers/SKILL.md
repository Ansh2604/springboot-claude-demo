---
name: testcontainers
description: Use when writing Spring Boot integration tests that need a real Postgres via Testcontainers. Triggers on "integration test", "@SpringBootTest", or repository/service tests that hit the database.
---

# Testcontainers Skill

## Test layout
- Integration tests live under `src/test/java` mirroring the main package, suffix with `IT` (e.g. `TaskControllerIT`).
- Unit tests use Mockito only — do **not** spin up a container for service unit tests.

## Standard scaffolding
```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaskControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## Rules
- Reuse one container per test class via `static` field — avoid per-method startup.
- Let Flyway run on context startup; do not bypass migrations with `ddl-auto=create`.
- Pin the Postgres image tag (`postgres:16-alpine`) — never use `latest`.
- Clean state between tests: prefer `@Transactional` rollback or explicit truncation. Do **not** drop/recreate the schema between tests.
- Assert against HTTP via `MockMvc`/`TestRestTemplate`, not by reading the repository directly — integration tests verify the full stack.
- Keep integration tests focused on golden-path + one or two error paths; exhaustive branch coverage belongs in unit tests.

## Verification
- `mvn verify` runs both unit and integration tests. Failing migrations or schema drift will surface here first.
