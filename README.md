# Bank Deposit Service

A REST-first bank deposit microservice built with Spring Boot 4.0 and Java 17.

## Quick Start

```bash
# 1) Build fast without tests (Docker not required)
./mvnw clean install -DskipTests

# 2) Run locally with in-memory H2
./mvnw spring-boot:run
```

Want PostgreSQL instead?

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Prerequisites

- Install **JDK 17**
- No Maven installation needed (use the project Maven Wrapper: `./mvnw`)
- Install **Docker Desktop** only if you want to:
	- run PostgreSQL locally with Docker Compose, or
	- run tests that require Testcontainers

If you only want to build without tests, Docker is optional.

## Running Locally

### With H2 (default, no setup needed)
```bash
./mvnw spring-boot:run
```

### With PostgreSQL
```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Running Tests
```bash
./mvnw test
```

## Build

### Full build (includes tests)
```bash
./mvnw clean install
```

### Build without tests (Docker not required)
```bash
./mvnw clean install -DskipTests
```
