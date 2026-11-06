# Library Management System

A small Java 25 / Spring Boot 4.1.2 REST API for managing and borrowing books.
Storage is an in-memory map: **all data is lost when the application stops**.
Authentication and concurrent modifications are out of scope.

## Requirements

- JDK 25, with `JAVA_HOME` pointing to it.
- Maven 3.9 or newer; check that `mvn -version` reports Java 25.
- Internet access for the first build.

## Run

```sh
mvn spring-boot:run
```

The API runs at `http://localhost:8080`. Stop it with Ctrl+C.

Alternatively, build and run the executable JAR:

```sh
mvn clean verify
java -jar target/library-management-system-0.0.1-SNAPSHOT.jar
```

## API contract

See [openapi.yaml](src/main/resources/static/openapi.yaml) for endpoints, schemas,
validation, and error responses. The running application serves the same contract
at `http://localhost:8080/openapi.yaml`.

Maven validates the contract and generates request/response DTOs before compiling.
To generate them separately:

```sh
mvn generate-sources
```

Generated files live under `target/generated-sources/openapi` and are not committed.
Edit the contract, not generated Java. Controllers, services, and the map-backed
repository remain handwritten.

The build uses OpenAPI Generator 7.27.0 and Swagger annotations 2.2.55. OAS 3.1.2
is used because the bundled parser does not support the newer OAS 3.2.1.

## Tests

```sh
mvn test          # JUnit unit tests
mvn clean verify  # Unit tests, packaging, and Cucumber HTTP integration tests
```

Integration tests start their own server on a random port; no running application
is required. Scenarios are in [books.feature](src/test/resources/features/books.feature).
Reports are written to `target/surefire-reports`, `target/failsafe-reports`, and
`target/cucumber-report.html`.

## Formatting

Spring Java Format automatically reformats handwritten Java during Maven builds.

```sh
mvn validate                  # Apply formatting only
mvn spring-javaformat:validate # Check formatting without modifying files
```
