# Transaction Management API

A production-grade RESTful microservice for managing financial transactions, built with **Spring Boot 3**, **Spring Security**, **Spring Data JPA**, and tested with **JUnit 5 + Mockito**.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2, Spring MVC |
| Security | Spring Security (role-based, stateless) |
| Persistence | Spring Data JPA, H2 (dev) / PostgreSQL / Oracle (prod) |
| Build | Maven |
| Testing | JUnit 5, Mockito, MockMvc |
| Docs | OpenAPI 3 / Swagger UI |
| Observability | Spring Actuator, structured logging (ELK-compatible) |

## Features

- **Idempotent transaction creation** — duplicate requests with the same key return the existing result without re-processing
- **Role-based access control** — `USER` can create/read; `ADMIN` can reverse transactions
- **Paginated queries** — account transaction history with date-range filtering
- **Global exception handling** — consistent error responses across all endpoints
- **OpenAPI documentation** — available at `/swagger-ui.html`
- **Actuator health endpoint** — `/actuator/health`

## Running Locally

```bash
mvn clean install
mvn spring-boot:run
```

API available at: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
H2 Console: `http://localhost:8080/h2-console`

## Sample Request

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -u user:password \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC-001",
    "amount": 500.00,
    "type": "CREDIT",
    "description": "Salary credit",
    "idempotencyKey": "unique-key-001"
  }'
```

## Running Tests

```bash
mvn test
```

## Production Swap Notes

- Replace H2 datasource with PostgreSQL/Oracle in `application.properties`
- Replace in-memory `UserDetailsService` with DB-backed implementation
- Add Kafka/SQS publisher in `TransactionService` for async settlement
- Enable structured JSON logging for ELK ingestion
