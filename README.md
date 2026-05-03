# task-manager

Spring Boot 3.3 task manager service.

## Stack

- Java 21
- Spring Boot 3.3 (Web, Data JPA, Validation, Actuator)
- Maven
- H2 (dev) / PostgreSQL (prod)
- Spring Retry + Resilience4j
- SpringDoc OpenAPI
- Lombok

## Build

```bash
mvn -DskipTests package
```

## Run

Dev profile (H2, in-memory) is active by default:

```bash
mvn spring-boot:run
```

Prod profile (PostgreSQL):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Required env vars for prod: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Endpoints

- App: http://localhost:8080
- Actuator: http://localhost:8080/actuator
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 console (dev only): http://localhost:8080/h2-console

## Package layout

```
com.demo.taskmanager
├── controller
├── service
├── repository
├── model
├── dto
├── exception
└── config
```

## Test

```bash
mvn test
```
