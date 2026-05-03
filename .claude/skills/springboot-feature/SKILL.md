---
name: springboot-feature
description: Use when implementing a new Spring Boot feature end-to-end. Triggers on "add a feature", "create an endpoint", or references to /specs files.
---

# Spring Boot Feature Skill

## Order of operations
1. Entity (JPA) in model/ — @Entity, @Id, @GeneratedValue(UUID), @CreatedDate, @LastModifiedDate
2. Repository — extends JpaRepository<Entity, UUID>, Spring Data query methods
3. Service — constructor injection, @Transactional on writes, @Transactional(readOnly=true) on reads
4. DTO records — separate Request/Response
5. Mapper — manual methods
6. Controller — @RestController, @Valid, full OpenAPI annotations
7. Tests — @WebMvcTest for controller, MockitoExtension for service
8. Integration test — @SpringBootTest + Testcontainers Postgres
