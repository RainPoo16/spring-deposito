# Bank Deposit Service

A REST-first bank deposit microservice built with Spring Boot 4.0 and Java 21.

## API Endpoints

### Customer Operations
- `POST /api/accounts` — Open a new account
- `GET /api/accounts/{accountNumber}` — Get account details
- `POST /api/accounts/{accountNumber}/credit` — Deposit funds
- `POST /api/accounts/{accountNumber}/debit` — Withdraw funds

### Admin Operations
- `POST /api/admin/accounts/{accountNumber}/freeze` — Freeze account
- `POST /api/admin/accounts/{accountNumber}/unfreeze` — Unfreeze account
- `POST /api/admin/accounts/{accountNumber}/close` — Close account

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
