# CLAUDE.md

Project conventions for the `task-manager` service. Follow these rules whenever you generate, edit, or review code in this repo.

## Stack

- Java 21
- Spring Boot 3.3
- Maven

## Architecture

- Package layout flows one direction: `controller` → `service` → `repository` → `model`.
- A higher layer may depend on a lower layer; lower layers must not depend on higher layers.
- DTOs live in `dto`, exceptions in `exception`, configuration in `config`.

## Code style

- All public methods must have Javadoc.
- Use constructor injection. Do not use `@Autowired` on fields or setters. Lombok's `@RequiredArgsConstructor` on `final` fields is preferred.
- DTOs must be Java `record` types (immutable). No mutable POJO DTOs.
- Validation annotations belong on DTO fields and controller method parameters.

## Error handling

- Provide a single global exception handler annotated with `@RestControllerAdvice`.
- Map domain exceptions to consistent HTTP responses there — do not catch-and-translate inside controllers.

## Resilience

- Every outbound HTTP call must be wrapped with Resilience4j `@Retry` and `@CircuitBreaker`.
- Configure named instances under `resilience4j.*` in `application.yml`; do not hardcode policy in code.

## Testing

- Every controller has a corresponding `@WebMvcTest`.
- Every service has a unit test with **≥ 80% line coverage**.
- Tests live under `src/test/java` mirroring the main package layout.
- Use `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ).

## Git workflow

- Never commit directly to `main`. All changes go through a pull request.
- Use **Conventional Commits** for every commit and PR title:
  - `feat:` — new feature
  - `fix:` — bug fix
  - `chore:` — tooling, deps, non-code
  - `docs:` — documentation only
  - `test:` — tests only
  - `refactor:` — code change that neither fixes a bug nor adds a feature
- Run `mvn verify` locally before every commit. Do not commit if it fails.
