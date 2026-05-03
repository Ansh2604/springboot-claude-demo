---
description: Implement a Spring Boot feature from a spec file in /specs
argument-hint: <spec-file-name>
---

Read the spec at specs/$ARGUMENTS.md. Then:
1. Generate the JPA entity in model/
2. Generate the repository interface in repository/
3. Generate the service class with constructor injection in service/
4. Generate the REST controller with OpenAPI annotations in controller/
5. Generate request/response DTOs as Java records in dto/
6. Update GlobalExceptionHandler in exception/ for any new errors
7. Generate unit tests for the service (Mockito)
8. Generate @WebMvcTest for the controller
9. Generate an integration test using @SpringBootTest + Testcontainers if Postgres is involved
10. Run `mvn verify` and fix any failures.
Follow all rules in CLAUDE.md.
